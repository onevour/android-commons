package com.onevour.core.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * REST code of android-commons, generated at build time:
 * <ul>
 *     <li>{@code <Repository>_Rest} for every @RestRepository: the repository without Proxy or
 *     reflection, the body type taken from the method; RestClient.create uses it.</li>
 *     <li>{@code <Class>Callbacks} for every class with @OnSuccess / @OnError / @OnAllSuccess:
 *     {@code <name>(target)} gives the listener that calls them while the screen is alive.</li>
 *     <li>{@code <Class>Api} for a class with @RestCallbacks: the repositories' methods without their
 *     listener, answered by the @OnSuccess / @OnError of the same name, types checked here.</li>
 * </ul>
 * Mistakes are build errors: a {name} without @Path, a @Body on a GET, two listeners, a callback of
 * the wrong type, a duplicated name.
 */
public class RestProcessor extends AbstractProcessor {

    private static final String REST = "com.onevour.core.rest";
    static final String REPOSITORY = REST + ".repository.RestRepository";
    static final String ON_SUCCESS = REST + ".annotations.OnSuccess";
    static final String ON_ERROR = REST + ".annotations.OnError";
    static final String ON_ALL_SUCCESS = REST + ".annotations.OnAllSuccess";
    static final String CALLBACKS = REST + ".annotations.RestCallbacks";

    private static final List<String> VERBS = Arrays.asList("Get", "Post", "Put", "Patch", "Delete");

    private static final ClassName REST_CALL = ClassName.get(REST + ".handler", "RestCall");
    private static final ClassName REST_TYPES = ClassName.get(REST + ".builder", "RestTypes");
    private static final ClassName REST_CLIENT = ClassName.get(REST + ".builder", "RestClient");
    private static final ClassName GENERATED_REPOSITORY = ClassName.get(REST + ".builder", "GeneratedRepository");
    private static final ClassName ALL_SUCCESS = ClassName.get(REST + ".builder", "AllSuccess");
    private static final ClassName HTTP_LISTENER = ClassName.get(REST + ".listener", "HttpListener");
    private static final ClassName TYPED_LISTENER = ClassName.get(REST + ".listener", "TypedHttpListener");
    private static final ClassName WEAK_REFERENCE = ClassName.get("java.lang.ref", "WeakReference");
    private static final String HTTP_HEADERS = REST + ".components.HttpHeaders";
    private static final String HTTP_RESPONSE = REST + ".models.HttpResponse";
    private static final String HTTP_ERROR = REST + ".models.HttpErrorResponse";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}/]+)}");

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
        return new HashSet<>(Arrays.asList(REPOSITORY, ON_SUCCESS, ON_ERROR, ON_ALL_SUCCESS, CALLBACKS));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement repository = elements.getTypeElement(REPOSITORY);
        if (repository != null) {
            for (Element element : round.getElementsAnnotatedWith(repository)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    error(element, "@RestRepository must be on an interface");
                    continue;
                }
                generateRepository((TypeElement) element);
            }
        }
        Set<TypeElement> screens = new LinkedHashSet<>();
        for (String name : Arrays.asList(ON_SUCCESS, ON_ERROR, ON_ALL_SUCCESS)) {
            TypeElement annotation = elements.getTypeElement(name);
            if (annotation == null) continue;
            for (Element element : round.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.METHOD) screens.add((TypeElement) element.getEnclosingElement());
            }
        }
        TypeElement callbacks = elements.getTypeElement(CALLBACKS);
        if (callbacks != null) {
            for (Element element : round.getElementsAnnotatedWith(callbacks)) screens.add((TypeElement) element);
        }
        for (TypeElement screen : screens) generateScreen(screen);
        return true;
    }

    /* =========================== <Repository>_Rest =========================== */

    private void generateRepository(TypeElement repository) {
        if (!repository.getTypeParameters().isEmpty()) {
            error(repository, "a @RestRepository must not be generic");
            return;
        }
        if (repository.getNestingKind() == NestingKind.MEMBER && repository.getModifiers().contains(Modifier.PRIVATE)) {
            error(repository, "a @RestRepository must not be private");
            return;
        }
        String generatedName = flatName(repository) + "_Rest";
        TypeSpec.Builder generated = TypeSpec.classBuilder(generatedName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get(repository))
                .addSuperinterface(GENERATED_REPOSITORY)
                .addOriginatingElement(repository)
                .addJavadoc("Generated by commons-sdk-processor: {@link $T} without Proxy; RestClient.create uses it.\n", ClassName.get(repository))
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
        boolean ok = true;
        int index = 0;
        for (ExecutableElement method : ElementFilter.methodsIn(repository.getEnclosedElements())) {
            if (!method.getModifiers().contains(Modifier.ABSTRACT)) continue;          // default / static methods stay as they are
            MethodSpec implementation = repositoryMethod(repository, method, generated, index++);
            if (implementation == null) {
                ok = false;
                continue;
            }
            generated.addMethod(implementation);
        }
        if (ok) write(repository, generated.build());
    }

    private MethodSpec repositoryMethod(TypeElement repository, ExecutableElement method, TypeSpec.Builder generated, int index) {
        AnnotationMirror verb = null;
        String verbName = null;
        for (String name : VERBS) {
            AnnotationMirror mirror = mirror(method, REST + ".annotations." + name);
            if (mirror == null) continue;
            if (verb != null) {
                error(method, "use one of @Get, @Post, @Put, @Patch, @Delete, not several");
                return null;
            }
            verb = mirror;
            verbName = name.toUpperCase();
        }
        if (verb == null) {
            error(method, "a repository method needs @Get, @Post, @Put, @Patch or @Delete");
            return null;
        }
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            error(method, "a repository method returns void: the answer comes to its HttpListener");
            return null;
        }
        Map<String, Object> config = values(verb);
        String url = config.get("key") + "" + config.get("url");
        int connect = (Integer) config.get("connect");
        int read = (Integer) config.get("read");
        String contentType = (String) config.get("contentType");

        CodeBlock.Builder call = CodeBlock.builder().add("new $T($S, $S, $L, $L, $S, $S)", REST_CALL, verbName, url, connect, read, contentType,
                repository.getSimpleName() + "." + method.getSimpleName());
        Set<String> pathNames = new LinkedHashSet<>();
        String headersParameter = null;
        String listenerParameter = null;
        TypeMirror bodyType = null;
        boolean ok = true;
        int bodies = 0;
        for (VariableElement parameter : method.getParameters()) {
            String name = parameter.getSimpleName().toString();
            boolean special = false;
            if (isType(parameter.asType(), HTTP_HEADERS)) {
                if (headersParameter != null) {
                    error(parameter, "only one HttpHeaders parameter");
                    ok = false;
                }
                headersParameter = name;
                special = true;
            }
            if (isListener(parameter.asType())) {
                if (listenerParameter != null) {
                    error(parameter, "only one HttpListener parameter");
                    ok = false;
                }
                listenerParameter = name;
                TypeMirror argument = listenerArgument(parameter.asType());
                if (argument != null && !usable(argument)) {
                    error(parameter, "the listener's body type must be a concrete type (no ?, no type variable): " + argument);
                    ok = false;
                } else {
                    bodyType = argument;
                }
                special = true;
            }
            for (AnnotationMirror annotation : parameter.getAnnotationMirrors()) {
                String type = ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString();
                String value = String.valueOf(values(annotation).get("value"));
                switch (type) {
                    case REST + ".annotations.Header":
                        call.add("\n.header($S, $L)", value, name);
                        special = true;
                        break;
                    case REST + ".annotations.Path":
                        call.add("\n.path($S, $L)", value, name);
                        pathNames.add(value);
                        special = true;
                        break;
                    case REST + ".annotations.Query":
                        call.add("\n.query($S, $L)", value, name);
                        special = true;
                        break;
                    case REST + ".annotations.Body":
                        call.add("\n.body($L)", name);
                        bodies++;
                        special = true;
                        break;
                    default:
                        break;
                }
            }
            if (!special) warning(parameter, "not sent: no @Path, @Query, @Header or @Body");
        }
        if (bodies > 1) {
            error(method, "only one @Body");
            ok = false;
        }
        if (bodies > 0 && "GET".equals(verbName)) {
            error(method, "a @Get request sends no body: remove @Body or use @Post");
            ok = false;
        }
        Set<String> placeholders = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(url);
        while (matcher.find()) placeholders.add(matcher.group(1));
        for (String placeholder : placeholders) {
            if (!pathNames.contains(placeholder)) {
                // a warning only: the Proxy sent it as it is too, the generated code keeps the same request
                warning(method, "{" + placeholder + "} in the url has no @Path(\"" + placeholder + "\") parameter: it is sent as it is");
            }
        }
        for (String path : pathNames) {
            if (!placeholders.contains(path)) {
                error(method, "@Path(\"" + path + "\") has no {" + path + "} in the url " + url);
                ok = false;
            }
        }
        if (!ok) return null;
        if (headersParameter != null) call.add("\n.headers($L)", headersParameter);
        String typeArgument = "null";
        if (bodyType != null) {
            String field = "TYPE_" + method.getSimpleName().toString().toUpperCase() + "_" + index;
            generated.addField(FieldSpec.builder(java.lang.reflect.Type.class, field, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(typeOf(bodyType)).build());
            typeArgument = field;
        }
        call.add("\n.send($L, $L)", typeArgument, listenerParameter == null ? "null" : listenerParameter);
        return MethodSpec.overriding(method).addStatement("$L", call.build()).build();
    }

    /* ======================= <Class>Callbacks and <Class>Api ======================= */

    private static final class Callback {
        final String name;
        final ExecutableElement success;
        /** The body type the listener parses; null when the method takes nothing (generic listener). */
        final TypeMirror body;
        /** The method takes the whole HttpResponse. */
        final boolean wholeResponse;
        ExecutableElement error;

        Callback(String name, ExecutableElement success, TypeMirror body, boolean wholeResponse) {
            this.name = name;
            this.success = success;
            this.body = body;
            this.wholeResponse = wholeResponse;
        }
    }

    private void generateScreen(TypeElement screen) {
        if (!checkScreen(screen)) return;
        Map<String, Callback> callbacks = new LinkedHashMap<>();
        boolean ok = true;
        List<ExecutableElement> methods = ElementFilter.methodsIn(screen.getEnclosedElements());
        for (ExecutableElement method : methods) {
            AnnotationMirror success = mirror(method, ON_SUCCESS);
            if (success == null) continue;
            String name = String.valueOf(values(success).get("value"));
            if (!checkCallable(method, "@OnSuccess") || !validName(method, name)) {
                ok = false;
                continue;
            }
            if (callbacks.containsKey(name)) {
                error(method, "two @OnSuccess(\"" + name + "\") in this class");
                ok = false;
                continue;
            }
            Callback callback = successCallback(name, method);
            if (callback == null) {
                ok = false;
                continue;
            }
            callbacks.put(name, callback);
        }
        for (ExecutableElement method : methods) {
            AnnotationMirror failure = mirror(method, ON_ERROR);
            if (failure == null) continue;
            String name = String.valueOf(values(failure).get("value"));
            Callback callback = callbacks.get(name);
            if (!checkCallable(method, "@OnError")) {
                ok = false;
                continue;
            }
            if (callback == null) {
                error(method, "@OnError(\"" + name + "\") without an @OnSuccess(\"" + name + "\")");
                ok = false;
                continue;
            }
            if (callback.error != null) {
                error(method, "two @OnError(\"" + name + "\") in this class");
                ok = false;
                continue;
            }
            if (!(method.getParameters().isEmpty()
                    || (method.getParameters().size() == 1 && isType(method.getParameters().get(0).asType(), HTTP_ERROR)))) {
                error(method, "@OnError takes no parameter or one HttpErrorResponse");
                ok = false;
                continue;
            }
            callback.error = method;
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();           // group id -> names
        Map<String, ExecutableElement> groupMethods = new LinkedHashMap<>();
        for (ExecutableElement method : methods) {
            AnnotationMirror all = mirror(method, ON_ALL_SUCCESS);
            if (all == null) continue;
            List<String> names = new ArrayList<>(new TreeSet<>(stringList(values(all).get("value"))));
            if (!checkCallable(method, "@OnAllSuccess")) {
                ok = false;
                continue;
            }
            if (!method.getParameters().isEmpty()) {
                error(method, "@OnAllSuccess takes no parameters");
                ok = false;
                continue;
            }
            if (names.size() < 2) {
                error(method, "@OnAllSuccess needs two calls or more; for one, use @OnSuccess");
                ok = false;
                continue;
            }
            for (String name : names) {
                if (!callbacks.containsKey(name)) {
                    error(method, "@OnAllSuccess: no @OnSuccess(\"" + name + "\") in this class");
                    ok = false;
                }
            }
            String id = String.join("+", names);
            if (groups.containsKey(id)) {
                error(method, "two @OnAllSuccess for " + names);
                ok = false;
                continue;
            }
            groups.put(id, names);
            groupMethods.put(id, method);
        }
        List<TypeElement> repositories = new ArrayList<>();
        AnnotationMirror restCallbacks = mirror(screen, CALLBACKS);
        if (restCallbacks != null) {
            for (Object value : (List<?>) values(restCallbacks).get("value")) {
                TypeMirror type = (TypeMirror) ((AnnotationValue) value).getValue();
                Element element = types.asElement(type);
                if (!(element instanceof TypeElement) || mirror(element, REPOSITORY) == null) {
                    error(screen, "@RestCallbacks: " + type + " is not a @RestRepository interface");
                    ok = false;
                    continue;
                }
                repositories.add((TypeElement) element);
            }
        }
        if (!ok) return;
        if (!callbacks.isEmpty()) writeCallbacks(screen, callbacks, groups, groupMethods);
        if (!repositories.isEmpty()) writeApi(screen, repositories, callbacks);
    }

    private Callback successCallback(String name, ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.isEmpty()) return new Callback(name, method, null, false);
        if (parameters.size() > 1) {
            error(method, "@OnSuccess takes one parameter: the body (e.g. UserResponse) or HttpResponse<UserResponse>, or none");
            return null;
        }
        TypeMirror type = parameters.get(0).asType();
        boolean whole = false;
        if (isType(type, HTTP_RESPONSE)) {
            List<? extends TypeMirror> arguments = ((DeclaredType) type).getTypeArguments();
            if (arguments.isEmpty()) {
                error(method, "say the body type: HttpResponse<UserResponse>");
                return null;
            }
            type = arguments.get(0);
            whole = true;
        }
        if (type.getKind().isPrimitive()) {
            error(method, "the body type must be an object type, e.g. Integer, not int");
            return null;
        }
        if (!usable(type)) {
            error(method, "the body type must be a concrete type (no ?, no type variable): " + type);
            return null;
        }
        return new Callback(name, method, type, whole);
    }

    private void writeCallbacks(TypeElement screen, Map<String, Callback> callbacks, Map<String, List<String>> groups,
                                Map<String, ExecutableElement> groupMethods) {
        ClassName target = ClassName.get(screen);
        TypeSpec.Builder generated = TypeSpec.classBuilder(flatName(screen) + "Callbacks")
                .addModifiers(Modifier.FINAL)
                .addOriginatingElement(screen)
                .addJavadoc("Generated by commons-sdk-processor: listeners calling the @OnSuccess / @OnError methods of {@link $T}.\n", target)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        boolean isPublic = screen.getModifiers().contains(Modifier.PUBLIC);
        if (isPublic) generated.addModifiers(Modifier.PUBLIC);
        generated.addMethod(aliveMethod(screen));
        for (Callback callback : callbacks.values()) {
            TypeName body = callback.body == null ? TypeVariableName.get("T") : TypeName.get(callback.body);
            MethodSpec.Builder method = MethodSpec.methodBuilder(callback.name)
                    .addModifiers(Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(HTTP_LISTENER, body))
                    .addParameter(target, "target")
                    .addJavadoc("The listener for {@link $T#$L}.\n", target, callback.success.getSimpleName());
            if (isPublic) method.addModifiers(Modifier.PUBLIC);
            if (callback.body == null) method.addTypeVariable(TypeVariableName.get("T"));
            method.addStatement("final $T<$T> screen = new $T<>(target)", WEAK_REFERENCE, target, WEAK_REFERENCE);
            CodeBlock.Builder success = CodeBlock.builder()
                    .addStatement("$T it = screen.get()", target)
                    .addStatement("if (!alive(it)) return");
            String argument = callback.success.getParameters().isEmpty() ? "" : callback.wholeResponse ? "response" : "response.getBody()";
            success.addStatement("it.$L($L)", callback.success.getSimpleName(), argument);
            CodeBlock.Builder failure = CodeBlock.builder()
                    .addStatement("$T it = screen.get()", target)
                    .addStatement("if (!alive(it)) return");
            for (Map.Entry<String, List<String>> group : groups.entrySet()) {
                if (!group.getValue().contains(callback.name)) continue;
                CodeBlock names = names(group.getValue());
                success.addStatement("if ($T.succeeded(it, $S, $S, $L)) it.$L()", ALL_SUCCESS, group.getKey(), callback.name, names,
                        groupMethods.get(group.getKey()).getSimpleName());
                failure.addStatement("$T.failed(it, $S)", ALL_SUCCESS, group.getKey());
            }
            if (callback.error == null) {
                failure.addStatement("$T.basic($S + error.getCode() + $S + error.getMessage())", ClassName.get(REST, "RestLog"),
                        "no @OnError(\"" + callback.name + "\"): ", " ");
            } else {
                failure.addStatement("it.$L($L)", callback.error.getSimpleName(), callback.error.getParameters().isEmpty() ? "" : "error");
            }
            method.addCode("return new $T<$T>($L, response -> {\n$>", TYPED_LISTENER, body,
                    callback.body == null ? CodeBlock.of("null") : typeOf(callback.body));
            method.addCode(success.build());
            method.addCode("$<}, error -> {\n$>");
            method.addCode(failure.build());
            method.addCode("$<});\n");
            generated.addMethod(method.build());
        }
        write(screen, generated.build());
    }

    /** Whether the screen can still take an answer: an Activity not finishing, a Fragment still added. */
    private MethodSpec aliveMethod(TypeElement screen) {
        ClassName target = ClassName.get(screen);
        MethodSpec.Builder alive = MethodSpec.methodBuilder("alive")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(target, "it");
        CodeBlock.Builder condition = CodeBlock.builder().add("it != null");
        if (isSubtype(screen, "android.app.Activity")) condition.add(" && !it.isFinishing() && !it.isDestroyed()");
        if (isSubtype(screen, "androidx.fragment.app.Fragment") || isSubtype(screen, "android.app.Fragment")) condition.add(" && it.isAdded()");
        return alive.addStatement("return $L", condition.build()).build();
    }

    private void writeApi(TypeElement screen, List<TypeElement> repositories, Map<String, Callback> callbacks) {
        ClassName target = ClassName.get(screen);
        ClassName callbacksClass = ClassName.get(ClassName.get(screen).packageName(), flatName(screen) + "Callbacks");
        TypeSpec.Builder generated = TypeSpec.classBuilder(flatName(screen) + "Api")
                .addModifiers(Modifier.FINAL)
                .addOriginatingElement(screen)
                .addJavadoc("Generated by commons-sdk-processor: the repositories of {@link $T}, answered by its @OnSuccess / @OnError.\n", target)
                .addField(target, "screen", Modifier.PRIVATE, Modifier.FINAL);
        boolean isPublic = screen.getModifiers().contains(Modifier.PUBLIC);
        if (isPublic) generated.addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder full = MethodSpec.constructorBuilder()
                .addJavadoc("With repositories of your own, e.g. mocks in a test.\n")
                .addParameter(target, "screen")
                .addStatement("this.screen = screen");
        MethodSpec.Builder simple = MethodSpec.constructorBuilder()
                .addParameter(target, "screen");
        List<CodeBlock> created = new ArrayList<>();
        Map<String, TypeElement> methodOwner = new LinkedHashMap<>();
        Map<String, TypeMirror> methodBody = new LinkedHashMap<>();
        boolean ok = true;
        for (TypeElement repository : repositories) {
            String field = lowerFirst(repository.getSimpleName().toString());
            generated.addField(ClassName.get(repository), field, Modifier.PRIVATE, Modifier.FINAL);
            full.addParameter(ClassName.get(repository), field).addStatement("this.$L = $L", field, field);
            created.add(CodeBlock.of("new $T().create($T.class)", REST_CLIENT, ClassName.get(repository)));
            for (ExecutableElement method : ElementFilter.methodsIn(repository.getEnclosedElements())) {
                if (!method.getModifiers().contains(Modifier.ABSTRACT)) continue;
                String name = method.getSimpleName().toString();
                TypeElement owner = methodOwner.get(name);
                if (owner != null && owner != repository) {
                    error(screen, "@RestCallbacks: " + owner.getSimpleName() + " and " + repository.getSimpleName()
                            + " both have " + name + "(): one name, one call");
                    ok = false;
                    continue;
                }
                methodOwner.put(name, repository);
                VariableElement listener = null;
                for (VariableElement parameter : method.getParameters()) {
                    if (isListener(parameter.asType())) listener = parameter;
                }
                TypeMirror body = listener == null ? null : listenerArgument(listener.asType());
                if (methodBody.containsKey(name) && body != null && methodBody.get(name) != null
                        && !types.isSameType(methodBody.get(name), body)) {
                    error(screen, "@RestCallbacks: the overloads of " + repository.getSimpleName() + "." + name + "() answer different types");
                    ok = false;
                }
                methodBody.put(name, body);
                Callback callback = callbacks.get(name);
                if (callback != null && listener != null && callback.body != null && body != null && !types.isSameType(callback.body, body)) {
                    error(callback.success, "@OnSuccess(\"" + name + "\") receives " + callback.body + " but "
                            + repository.getSimpleName() + "." + name + "() answers " + body);
                    ok = false;
                    continue;
                }
                if (callback == null && listener != null) {
                    warning(screen, "no @OnSuccess(\"" + name + "\") for " + repository.getSimpleName() + "." + name + "(): its answer is only logged");
                }
                generated.addMethod(apiMethod(method, field, listener, body, callback, callbacksClass, isPublic));
            }
        }
        for (Callback callback : callbacks.values()) {
            if (!methodOwner.containsKey(callback.name)) {
                messager.printMessage(Diagnostic.Kind.NOTE, "@OnSuccess(\"" + callback.name + "\") is not a method of the @RestCallbacks repositories:"
                        + " use it with " + callbacksClass.simpleName() + "." + callback.name + "(this)", callback.success);
            }
        }
        if (!ok) return;
        simple.addStatement("this(screen, $L)", CodeBlock.join(created, ", "));
        if (isPublic) {
            simple.addModifiers(Modifier.PUBLIC);
            full.addModifiers(Modifier.PUBLIC);
        }
        generated.addMethod(simple.build()).addMethod(full.build());
        write(screen, generated.build());
    }

    private MethodSpec apiMethod(ExecutableElement method, String field, VariableElement listener, TypeMirror body,
                                 Callback callback, ClassName callbacksClass, boolean isPublic) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addJavadoc("{@link $T#$L}, answered by @OnSuccess(\"$L\").\n", ClassName.get((TypeElement) method.getEnclosingElement()),
                        method.getSimpleName(), method.getSimpleName());
        if (isPublic) builder.addModifiers(Modifier.PUBLIC);
        List<CodeBlock> arguments = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            if (parameter == listener) {
                if (callback != null) {
                    arguments.add(CodeBlock.of("$T.$L(screen)", callbacksClass, callback.name));
                } else {
                    arguments.add(CodeBlock.of("new $T<>($L, null, null)", TYPED_LISTENER, body == null ? CodeBlock.of("null") : typeOf(body)));
                }
                continue;
            }
            builder.addParameter(ParameterSpec.get(parameter));
            arguments.add(CodeBlock.of("$L", parameter.getSimpleName()));
        }
        builder.varargs(method.isVarArgs() && listener == null);
        return builder.addStatement("$L.$L($L)", field, method.getSimpleName(), CodeBlock.join(arguments, ", ")).build();
    }

    /* =========================== helpers =========================== */

    private boolean checkScreen(TypeElement screen) {
        if (screen.getKind() != ElementKind.CLASS) {
            error(screen, "@OnSuccess / @OnError / @RestCallbacks belong to a class");
            return false;
        }
        if (screen.getModifiers().contains(Modifier.PRIVATE)) {
            error(screen, "a class with @OnSuccess / @OnError must not be private");
            return false;
        }
        if (screen.getNestingKind() == NestingKind.MEMBER && !screen.getModifiers().contains(Modifier.STATIC)) {
            error(screen, "a nested class with @OnSuccess / @OnError must be static");
            return false;
        }
        if (!screen.getTypeParameters().isEmpty()) {
            error(screen, "a class with @OnSuccess / @OnError must not be generic");
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

    private boolean validName(ExecutableElement method, String name) {
        if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name) || "alive".equals(name)) {
            error(method, "\"" + name + "\" must be a Java name: it becomes the generated method's name");
            return false;
        }
        return true;
    }

    /** A Type expression for a type known at build time: Foo.class, RestTypes.of(List.class, Foo.class)... */
    private CodeBlock typeOf(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            TypeMirror component = ((ArrayType) type).getComponentType();
            if (component.getKind().isPrimitive() || ((component instanceof DeclaredType) && ((DeclaredType) component).getTypeArguments().isEmpty())) {
                return CodeBlock.of("$T.class", TypeName.get(type));
            }
            return CodeBlock.of("$T.array($L)", REST_TYPES, typeOf(component));
        }
        DeclaredType declared = (DeclaredType) type;
        ClassName raw = ClassName.get((TypeElement) declared.asElement());
        if (declared.getTypeArguments().isEmpty()) return CodeBlock.of("$T.class", raw);
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.of($T.class", REST_TYPES, raw);
        for (TypeMirror argument : declared.getTypeArguments()) builder.add(", $L", typeOf(argument));
        return builder.add(")").build();
    }

    private boolean usable(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) return usable(((ArrayType) type).getComponentType());
        if (type.getKind().isPrimitive()) return true;
        if (type.getKind() != TypeKind.DECLARED) return false;
        for (TypeMirror argument : ((DeclaredType) type).getTypeArguments()) {
            if (argument.getKind().isPrimitive() || !usable(argument)) return false;
        }
        return true;
    }

    private boolean isListener(TypeMirror type) {
        TypeElement listener = elements.getTypeElement(HTTP_LISTENER.canonicalName());
        return listener != null && type.getKind() == TypeKind.DECLARED
                && types.isAssignable(types.erasure(type), types.erasure(listener.asType()));
    }

    /** X of HttpListener<X>; null for a raw listener or a subtype. */
    private TypeMirror listenerArgument(TypeMirror type) {
        if (!isType(type, HTTP_LISTENER.canonicalName())) return null;
        List<? extends TypeMirror> arguments = ((DeclaredType) type).getTypeArguments();
        return arguments.isEmpty() ? null : arguments.get(0);
    }

    private boolean isType(TypeMirror type, String qualifiedName) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals(qualifiedName);
    }

    private boolean isSubtype(TypeElement element, String qualifiedName) {
        TypeElement other = elements.getTypeElement(qualifiedName);
        return other != null && types.isSubtype(types.erasure(element.asType()), types.erasure(other.asType()));
    }

    private AnnotationMirror mirror(Element element, String qualifiedName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(qualifiedName)) return mirror;
        }
        return null;
    }

    /** Values with defaults, by name. */
    private Map<String, Object> values(AnnotationMirror mirror) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elements.getElementValuesWithDefaults(mirror).entrySet()) {
            values.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
        }
        return values;
    }

    private static List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) result.add(String.valueOf(((AnnotationValue) item).getValue()));
        } else if (value != null) {
            result.add(String.valueOf(value));
        }
        return result;
    }

    private static CodeBlock names(List<String> names) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = 0; i < names.size(); i++) builder.add(i == 0 ? "$S" : ", $S", names.get(i));
        return builder.build();
    }

    /** Outer_Inner for nested types. */
    private static String flatName(TypeElement type) {
        List<String> names = new ArrayList<>();
        Element current = type;
        while (current instanceof TypeElement) {
            names.add(0, current.getSimpleName().toString());
            current = current.getEnclosingElement();
        }
        return String.join("_", names);
    }

    private static String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void write(TypeElement origin, TypeSpec type) {
        try {
            JavaFile.builder(ClassName.get(origin).packageName(), type)
                    .addFileComment("Generated by commons-sdk-processor. Do not edit.")
                    .build()
                    .writeTo(filer);
        } catch (IOException e) {
            error(origin, "cannot write " + type.name + ": " + e.getMessage());
        }
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void warning(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message, element);
    }
}
