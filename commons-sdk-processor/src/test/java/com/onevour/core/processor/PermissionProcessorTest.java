package com.onevour.core.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

/** What the processor generates for @NeedsPermission / @OnPermissionDenied, and what it refuses. */
public class PermissionProcessorTest {

    /** The library pieces the generated code and the screens use, without Android. */
    private static final JavaFileObject NEEDS = JavaFileObjects.forSourceLines("com.onevour.core.permission.NeedsPermission",
            "package com.onevour.core.permission;",
            "public @interface NeedsPermission { String[] value(); }");

    private static final JavaFileObject DENIED = JavaFileObjects.forSourceLines("com.onevour.core.permission.OnPermissionDenied",
            "package com.onevour.core.permission;",
            "public @interface OnPermissionDenied { String[] value(); }");

    private static final JavaFileObject HELPER = JavaFileObjects.forSourceLines("com.onevour.core.utilities.commons.PermissionHelper",
            "package com.onevour.core.utilities.commons;",
            "import java.util.Set;",
            "public class PermissionHelper {",
            "    public interface Callback { void granted(); void denied(Set<String> deniedGroups); }",
            "    public void run(Object screen, Callback callback, String... groups) { }",
            "}");

    private Compilation compile(String... screen) {
        return javac().withProcessors(new PermissionProcessor())
                .compile(NEEDS, DENIED, HELPER, JavaFileObjects.forSourceLines("app.CheckInActivity", screen));
    }

    @Test
    public void generatesTheCheck_withArguments_multipleGroups_andTheDeniedGroups() {
        Compilation compilation = compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "import java.util.Set;",
                "public class CheckInActivity {",
                "    @NeedsPermission({\"location\", \"camera\"})",
                "    void save(String customer, int qty) { }",
                "    @OnPermissionDenied({\"camera\", \"location\"})",   // other order: same groups
                "    void onDenied(Set<String> groups) { }",
                "    @NeedsPermission(\"location\")",
                "    public void refresh() { }",
                "}");
        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("static void saveWithPermissionCheck(final CheckInActivity target, final String customer,");
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("if (it != null) it.save(customer, qty);");
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("if (it != null) it.onDenied(deniedGroups);");
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("\"camera\", \"location\")");
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("public static void refreshWithPermissionCheck(final CheckInActivity target)");
    }

    @Test
    public void deniedWithoutParameters_andNoDeniedMethod_compile() {
        Compilation compilation = compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    @NeedsPermission(\"camera\") void photo() { }",
                "    @OnPermissionDenied(\"camera\") void noCamera() { }",
                "    @NeedsPermission(\"storage\") void pick() { }",
                "}");
        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("app.CheckInActivityPermissions")
                .contentsAsUtf8String().contains("if (it != null) it.noCamera();");
    }

    @Test
    public void nestedClasses_getOuter_InnerPermissions() {
        Compilation compilation = compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    public static class PhotoFragment {",
                "        @NeedsPermission(\"camera\") void photo() { }",
                "    }",
                "}");
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("app.CheckInActivity_PhotoFragmentPermissions");
    }

    @Test
    public void privateMethod_isABuildError() {
        assertThat(compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    @NeedsPermission(\"location\") private void save() { }",
                "}")).hadErrorContaining("must not be private");
    }

    @Test
    public void unknownGroup_isABuildError() {
        assertThat(compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    @NeedsPermission(\"gps\") void save() { }",
                "}")).hadErrorContaining("unknown permission group [gps]");
    }

    @Test
    public void deniedWithAWrongParameter_isABuildError() {
        assertThat(compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    @NeedsPermission(\"camera\") void photo() { }",
                "    @OnPermissionDenied(\"camera\") void noCamera(int code) { }",
                "}")).hadErrorContaining("takes no parameter, or one Set<String>");
    }

    @Test
    public void checkedException_isABuildError() {
        assertThat(compile(
                "package app;",
                "import com.onevour.core.permission.*;",
                "public class CheckInActivity {",
                "    @NeedsPermission(\"camera\") void photo() throws java.io.IOException { }",
                "}")).hadErrorContaining("must not throw checked exceptions");
    }
}
