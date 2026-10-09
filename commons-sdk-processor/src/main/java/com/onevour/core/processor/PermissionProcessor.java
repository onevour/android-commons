package com.onevour.core.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Generates, for every class with {@code @NeedsPermission} methods, a {@code <Class>Permissions}
 * class next to it with one {@code <method>WithPermissionCheck(target, args...)} per method: it runs
 * the method when its permission groups are allowed already, otherwise asks them through
 * PermissionHelper.run and runs the method once all are allowed, or the {@code @OnPermissionDenied}
 * method of the same groups when one is refused.
 * <p>
 * Mistakes are build errors: a private or static method, an unknown group, a denied method with
 * parameters, two denied methods for the same groups.
 */
public class PermissionProcessor extends AbstractProcessor {

    static final String NEEDS = "com.onevour.core.permission.NeedsPermission";

    static final String DENIED = "com.onevour.core.permission.OnPermissionDenied";

    /** Must match PermissionHelper.GROUPS. */
    static final Set<String> GROUPS = new HashSet<>(Arrays.asList("location", "camera", "storage", "bluetooth", "phone", "notifications"));

    private static final ClassName PERMISSION_HELPER = ClassName.get("com.onevour.core.utilities.commons", "PermissionHelper");

    private static final ClassName CALLBACK = PERMISSION_HELPER.nestedClass("Callback");

    private static final ClassName WEAK_REFERENCE = ClassName.get("java.lang.ref", "WeakReference");

    private Messager messager;

    private Filer filer;

    private Elements elements;

    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        super.init(environment);
        messager = environment.getMessager();
        filer = environment.getFiler();
        elements = environment.getElementUtils();
        types = environment.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(NEEDS, DENIED));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement needsType = elements.getTypeElement(NEEDS);
        TypeElement deniedType = elements.getTypeElement(DENIED);
        if (needsType == null) return false;
        Map<TypeElement, List<ExecutableElement>> needsByClass = new LinkedHashMap<>();
        for (Element element : round.getElementsAnnotatedWith(needsType)) {
            if (element.getKind() != ElementKind.METHOD) continue;
            needsByClass.computeIfAbsent((TypeElement) element.getEnclosingElement(), key -> new ArrayList<>()).add((ExecutableElement) element);
        }
        Map<TypeElement, List<ExecutableElement>> deniedByClass = new LinkedHashMap<>();
        if (deniedType != null) {
            for (Element element : round.getElementsAnnotatedWith(deniedType)) {
                if (element.getKind() != ElementKind.METHOD) continue;
                deniedByClass.computeIfAbsent((TypeElement) element.getEnclosingElement(), key -> new ArrayList<>()).add((ExecutableElement) element);
            }
        }
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : deniedByClass.entrySet()) {
            if (!needsByClass.containsKey(entry.getKey())) {
                for (ExecutableElement method : entry.getValue()) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "@OnPermissionDenied without a @NeedsPermission method in this class", method);
                }
            }
        }
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : needsByClass.entrySet()) {
            generate(entry.getKey(), entry.getValue(), deniedByClass.getOrDefault(entry.getKey(), Collections.emptyList()));
        }
        return true;
    }

    private void generate(TypeElement target, List<ExecutableElement> needsMethods, List<ExecutableElement> deniedMethods) {
        if (!checkTarget(target)) return;
        Map<Set<String>, ExecutableElement> deniedByGroups = new LinkedHashMap<>();
        boolean ok = true;
        for (ExecutableElement denied : deniedMethods) {
            Set<String> groups = groups(denied, DENIED);
            if (groups == null || !checkCallable(denied, "@OnPermissionDenied")) {
                ok = false;
                continue;
            }
            if (!deniedParametersOk(denied)) {
                error(denied, "@OnPermissionDenied method takes no parameter, or one Set<String> (the groups not allowed)");
                ok = false;
                continue;
            }
            if (deniedByGroups.containsKey(groups)) {
                error(denied, "two @OnPermissionDenied methods for the groups " + groups);
                ok = false;
                continue;
            }
            deniedByGroups.put(groups, denied);
        }
        Set<Set<String>> used = new HashSet<>();
        TypeName targetType = ClassName.get(target);
        String generatedName = generatedName(target);
        TypeSpec.Builder generated = TypeSpec.classBuilder(generatedName)
                .addModifiers(Modifier.FINAL)
                .addJavadoc("Generated by commons-sdk-processor for {@link $T}: call these instead of the @NeedsPermission methods.\n", targetType)
                .addOriginatingElement(target)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        if (target.getModifiers().contains(Modifier.PUBLIC)) generated.addModifiers(Modifier.PUBLIC);
        for (ExecutableElement method : needsMethods) {
            Set<String> groups = groups(method, NEEDS);
            if (groups == null || !checkCallable(method, "@NeedsPermission")) {
                ok = false;
                continue;
            }
            if (!method.getThrownTypes().isEmpty() && throwsChecked(method)) {
                error(method, "@NeedsPermission method must not throw checked exceptions: it may run later, from the permission dialog's answer");
                ok = false;
                continue;
            }
            ExecutableElement denied = deniedByGroups.get(groups);
            if (denied != null) used.add(groups);
            generated.addMethod(checkMethod(target, targetType, method, groups, denied));
        }
        for (Map.Entry<Set<String>, ExecutableElement> entry : deniedByGroups.entrySet()) {
            if (!used.contains(entry.getKey())) {
                messager.printMessage(Diagnostic.Kind.WARNING, "no @NeedsPermission method for the groups " + entry.getKey(), entry.getValue());
            }
        }
        if (!ok) return;
        String packageName = elements.getPackageOf(target).getQualifiedName().toString();
        try {
            JavaFile.builder(packageName, generated.build())
                    .addFileComment("Generated by commons-sdk-processor. Do not edit.")
                    .build()
                    .writeTo(filer);
        } catch (IOException e) {
            error(target, "cannot write " + generatedName + ": " + e.getMessage());
        }
    }

    /**
     * public static void saveWithPermissionCheck(final Screen target, final A a) {
     *     final WeakReference<Screen> screen = new WeakReference<>(target);
     *     new PermissionHelper().run(target, new PermissionHelper.Callback() {
     *         public void granted() { Screen it = screen.get(); if (it != null) it.save(a); }
     *         public void denied(Set<String> deniedGroups) { Screen it = screen.get(); if (it != null) it.onDenied(deniedGroups); }
     *     }, "location", "camera");
     * }
     */
    private MethodSpec checkMethod(TypeElement target, TypeName targetType, ExecutableElement method, Set<String> groups, ExecutableElement denied) {
        String name = method.getSimpleName().toString();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(name + "WithPermissionCheck")
                .addModifiers(Modifier.STATIC)
                .addJavadoc("Runs {@link $T#$L} when $L allowed, else asks first.\n", targetType, name, groups);
        if (!method.getModifiers().contains(Modifier.PRIVATE) && target.getModifiers().contains(Modifier.PUBLIC)
                && method.getModifiers().contains(Modifier.PUBLIC)) {
            builder.addModifiers(Modifier.PUBLIC);
        }
        for (TypeParameterElement typeParameter : method.getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameter));
        }
        builder.addParameter(ParameterSpec.builder(targetType, "target", Modifier.FINAL).build());
        List<String> arguments = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            String parameterName = parameter.getSimpleName().toString();
            if (parameterName.equals("target") || parameterName.equals("screen") || parameterName.equals("it")) {
                parameterName = parameterName + "_";
            }
            builder.addParameter(ParameterSpec.builder(TypeName.get(parameter.asType()), parameterName, Modifier.FINAL).build());
            arguments.add(parameterName);
        }
        builder.varargs(method.isVarArgs());
        TypeName reference = ParameterizedTypeName.get(WEAK_REFERENCE, targetType);
        builder.addStatement("final $T screen = new $T<>(target)", reference, WEAK_REFERENCE);
        CodeBlock deniedBody = denied == null
                ? CodeBlock.of("// no @OnPermissionDenied for $L: nothing to do\n", groups)
                : CodeBlock.builder()
                .addStatement("$T it = screen.get()", targetType)
                .addStatement("if (it != null) it.$L($L)", denied.getSimpleName(), denied.getParameters().isEmpty() ? "" : "deniedGroups")
                .build();
        TypeSpec callback = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(CALLBACK)
                .addMethod(MethodSpec.methodBuilder("granted")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("$T it = screen.get()", targetType)
                        .addStatement("if (it != null) it.$L($L)", name, String.join(", ", arguments))
                        .build())
                .addMethod(MethodSpec.methodBuilder("denied")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(String.class)), "deniedGroups")
                        .addCode(deniedBody)
                        .build())
                .build();
        CodeBlock.Builder groupArguments = CodeBlock.builder();
        for (String group : groups) groupArguments.add(", $S", group);
        builder.addStatement("new $T().run(target, $L$L)", PERMISSION_HELPER, callback, groupArguments.build());
        return builder.build();
    }

    /** The groups of the annotation on this method, sorted; null (with an error) when wrong. */
    private Set<String> groups(ExecutableElement method, String annotation) {
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            if (!((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(annotation)) continue;
            Set<String> groups = new TreeSet<>();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                if (!entry.getKey().getSimpleName().contentEquals("value")) continue;
                Object value = entry.getValue().getValue();
                if (value instanceof List) {
                    for (Object item : (List<?>) value) groups.add(String.valueOf(((AnnotationValue) item).getValue()));
                } else {
                    groups.add(String.valueOf(value));
                }
            }
            if (groups.isEmpty()) {
                error(method, "give at least one permission group, e.g. @" + simple(annotation) + "(\"location\")");
                return null;
            }
            Set<String> unknown = new LinkedHashSet<>(groups);
            unknown.removeAll(GROUPS);
            if (!unknown.isEmpty()) {
                error(method, "unknown permission group " + unknown + "; known: " + new TreeSet<>(GROUPS));
                return null;
            }
            return Collections.unmodifiableSet(groups);
        }
        return null;
    }

    private boolean checkTarget(TypeElement target) {
        if (target.getKind() != ElementKind.CLASS) {
            error(target, "@NeedsPermission methods must be in a class");
            return false;
        }
        if (target.getModifiers().contains(Modifier.PRIVATE)) {
            error(target, "a class with @NeedsPermission methods must not be private");
            return false;
        }
        if (target.getNestingKind() == NestingKind.MEMBER && !target.getModifiers().contains(Modifier.STATIC)) {
            error(target, "a nested class with @NeedsPermission methods must be static");
            return false;
        }
        if (!target.getTypeParameters().isEmpty()) {
            error(target, "a class with @NeedsPermission methods must not be generic");
            return false;
        }
        return true;
    }

    private boolean checkCallable(ExecutableElement method, String annotation) {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            error(method, annotation + " method must not be private: the generated class calls it");
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            error(method, annotation + " method must not be static");
            return false;
        }
        return true;
    }

    /** None, or one parameter a Set<String> can be passed to (Set, Collection, Iterable of String). */
    private boolean deniedParametersOk(ExecutableElement method) {
        if (method.getParameters().isEmpty()) return true;
        if (method.getParameters().size() != 1) return false;
        TypeMirror setOfString = types.getDeclaredType(elements.getTypeElement("java.util.Set"),
                elements.getTypeElement("java.lang.String").asType());
        return types.isAssignable(setOfString, method.getParameters().get(0).asType());
    }

    private boolean throwsChecked(ExecutableElement method) {
        TypeMirror runtime = elements.getTypeElement("java.lang.RuntimeException").asType();
        TypeMirror error = elements.getTypeElement("java.lang.Error").asType();
        for (TypeMirror thrown : method.getThrownTypes()) {
            if (thrown.getKind() != TypeKind.DECLARED) return true;
            if (!types.isSubtype(thrown, runtime) && !types.isSubtype(thrown, error)) return true;
        }
        return false;
    }

    /** Outer_InnerPermissions for nested classes. */
    private static String generatedName(TypeElement target) {
        List<String> names = new ArrayList<>();
        Element current = target;
        while (current instanceof TypeElement) {
            names.add(0, current.getSimpleName().toString());
            current = current.getEnclosingElement();
        }
        return String.join("_", names) + "Permissions";
    }

    private static String simple(String annotation) {
        return annotation.substring(annotation.lastIndexOf('.') + 1);
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
