package com.onevour.core.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaFileObject;

/** What RestProcessor generates for repositories and screens, and what it refuses. */
public class RestProcessorTest {

    private static JavaFileObject source(String name, String... lines) {
        return JavaFileObjects.forSourceLines(name, lines);
    }

    /** The library pieces the generated code uses, without Android. */
    private static final List<JavaFileObject> LIBRARY = Arrays.asList(
            source("com.onevour.core.rest.repository.RestRepository", "package com.onevour.core.rest.repository;",
                    "public @interface RestRepository { }"),
            source("com.onevour.core.rest.annotations.Get", "package com.onevour.core.rest.annotations;",
                    "public @interface Get { String key() default \"\"; String url() default \"\"; int connect() default 0; int read() default 0; String contentType() default \"application/json\"; }"),
            source("com.onevour.core.rest.annotations.Post", "package com.onevour.core.rest.annotations;",
                    "public @interface Post { String key() default \"\"; String url() default \"\"; int connect() default 0; int read() default 0; String contentType() default \"application/json\"; }"),
            source("com.onevour.core.rest.annotations.Put", "package com.onevour.core.rest.annotations;",
                    "public @interface Put { String key() default \"\"; String url() default \"\"; int connect() default 0; int read() default 0; String contentType() default \"application/json\"; }"),
            source("com.onevour.core.rest.annotations.Patch", "package com.onevour.core.rest.annotations;",
                    "public @interface Patch { String key() default \"\"; String url() default \"\"; int connect() default 0; int read() default 0; String contentType() default \"application/json\"; }"),
            source("com.onevour.core.rest.annotations.Delete", "package com.onevour.core.rest.annotations;",
                    "public @interface Delete { String key() default \"\"; String url() default \"\"; int connect() default 0; int read() default 0; String contentType() default \"application/json\"; }"),
            source("com.onevour.core.rest.annotations.Path", "package com.onevour.core.rest.annotations;", "public @interface Path { String value(); }"),
            source("com.onevour.core.rest.annotations.Query", "package com.onevour.core.rest.annotations;", "public @interface Query { String value(); }"),
            source("com.onevour.core.rest.annotations.Header", "package com.onevour.core.rest.annotations;", "public @interface Header { String value(); }"),
            source("com.onevour.core.rest.annotations.Body", "package com.onevour.core.rest.annotations;", "public @interface Body { }"),
            source("com.onevour.core.rest.annotations.OnSuccess", "package com.onevour.core.rest.annotations;", "public @interface OnSuccess { String value(); }"),
            source("com.onevour.core.rest.annotations.OnError", "package com.onevour.core.rest.annotations;", "public @interface OnError { String value(); }"),
            source("com.onevour.core.rest.annotations.OnAllSuccess", "package com.onevour.core.rest.annotations;", "public @interface OnAllSuccess { String[] value(); }"),
            source("com.onevour.core.rest.annotations.RestCallbacks", "package com.onevour.core.rest.annotations;", "public @interface RestCallbacks { Class<?>[] value(); }"),
            source("com.onevour.core.rest.components.HttpHeaders", "package com.onevour.core.rest.components;", "public class HttpHeaders { }"),
            source("com.onevour.core.rest.models.HttpResponse", "package com.onevour.core.rest.models;",
                    "public class HttpResponse<T> { public T getBody() { return null; } public int getCode() { return 0; } }"),
            source("com.onevour.core.rest.models.HttpErrorResponse", "package com.onevour.core.rest.models;",
                    "public class HttpErrorResponse { public int getCode() { return 0; } public String getMessage() { return null; } }"),
            source("com.onevour.core.rest.listener.HttpListener", "package com.onevour.core.rest.listener;",
                    "import com.onevour.core.rest.models.*;",
                    "public interface HttpListener<T> { void onSuccess(HttpResponse<T> r); void onError(HttpErrorResponse e);",
                    "  interface OnSuccess<T> { void onSuccess(HttpResponse<T> r); } interface OnError { void onError(HttpErrorResponse e); } }"),
            source("com.onevour.core.rest.listener.TypedHttpListener", "package com.onevour.core.rest.listener;",
                    "import com.onevour.core.rest.models.*; import java.lang.reflect.Type;",
                    "public class TypedHttpListener<T> implements HttpListener<T> {",
                    "  public TypedHttpListener(Type t, OnSuccess<T> s, OnError e) { }",
                    "  public void onSuccess(HttpResponse<T> r) { } public void onError(HttpErrorResponse e) { } }"),
            source("com.onevour.core.rest.handler.RestCall", "package com.onevour.core.rest.handler;",
                    "import com.onevour.core.rest.components.HttpHeaders; import com.onevour.core.rest.listener.HttpListener; import java.lang.reflect.Type;",
                    "public final class RestCall { public RestCall(String m, String u, int c, int r, String ct, String s) { }",
                    "  public RestCall path(String n, Object v) { return this; } public RestCall query(String n, Object v) { return this; }",
                    "  public RestCall header(String n, Object v) { return this; } public RestCall headers(HttpHeaders h) { return this; }",
                    "  public RestCall body(Object v) { return this; } public <T> void send(Type t, HttpListener<T> l) { } }"),
            source("com.onevour.core.rest.builder.RestTypes", "package com.onevour.core.rest.builder;", "import java.lang.reflect.Type;",
                    "public final class RestTypes { public static Type of(Class<?> raw, Type... a) { return raw; } public static Type array(Type c) { return c; } }"),
            source("com.onevour.core.rest.builder.GeneratedRepository", "package com.onevour.core.rest.builder;", "public interface GeneratedRepository { }"),
            source("com.onevour.core.rest.builder.RestClient", "package com.onevour.core.rest.builder;",
                    "public final class RestClient { public <T> T create(Class<T> c) { return null; } }"),
            source("com.onevour.core.rest.builder.AllSuccess", "package com.onevour.core.rest.builder;",
                    "public final class AllSuccess { public static boolean succeeded(Object s, String g, String c, String... all) { return false; }",
                    "  public static void failed(Object s, String g) { } }"),
            source("com.onevour.core.rest.RestLog", "package com.onevour.core.rest;", "public final class RestLog { public static void basic(String m) { } }"),
            source("app.User", "package app;", "public class User { }"));

    private static final JavaFileObject REPOSITORY = source("app.UserRepository",
            "package app;",
            "import com.onevour.core.rest.annotations.*;",
            "import com.onevour.core.rest.repository.RestRepository;",
            "import com.onevour.core.rest.listener.HttpListener;",
            "import com.onevour.core.rest.components.HttpHeaders;",
            "import java.util.List;",
            "@RestRepository",
            "public interface UserRepository {",
            "    String BASE = \"https://api.example.com\";",
            "    @Get(key = BASE, url = \"/users\") void search(@Query(\"name\") String name, @Query(\"page\") int page, HttpListener<List<User>> callback);",
            "    @Get(key = BASE, url = \"/users/{id}\", connect = 5, read = 10) void detail(@Path(\"id\") String id, @Header(\"Authorization\") String token, HttpHeaders extra, HttpListener<User> callback);",
            "    @Post(key = BASE, url = \"/users\") void create(@Body User user, HttpListener<User> callback);",
            "    @Delete(key = BASE, url = \"/users/{id}\") void remove(@Path(\"id\") long id, HttpListener<String> callback);",
            "}");

    private Compilation compile(JavaFileObject... sources) {
        List<JavaFileObject> all = new ArrayList<>(LIBRARY);
        all.addAll(Arrays.asList(sources));
        return javac().withProcessors(new RestProcessor()).compile(all);
    }

    private static JavaFileObject screen(String... lines) {
        return source("app.UserActivity", lines);
    }

    // ------------------------------------------------------------------ repositories

    @Test
    public void repository_isImplementedWithoutProxy() {
        Compilation compilation = compile(REPOSITORY);
        assertThat(compilation).succeededWithoutWarnings();
        String generated = "app.UserRepository_Rest";
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String()
                .contains("public final class UserRepository_Rest implements UserRepository, GeneratedRepository");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String()
                .contains("new RestCall(\"GET\", \"https://api.example.com/users\", 0, 0, \"application/json\", \"UserRepository.search\")");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains(".query(\"name\", name)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("RestTypes.of(List.class, User.class)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("\"https://api.example.com/users/{id}\", 5, 10,");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains(".path(\"id\", id)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains(".header(\"Authorization\", token)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains(".headers(extra)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains(".body(user)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("String.class");
    }

    private static JavaFileObject badRepository(String method) {
        return source("app.BadRepository", "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "import com.onevour.core.rest.repository.RestRepository;",
                "import com.onevour.core.rest.listener.HttpListener;",
                "import java.util.List;",
                "@RestRepository public interface BadRepository {", method, "}");
    }

    @Test
    public void placeholderWithoutPath_isABuildError() {
        assertThat(compile(badRepository("@Get(url = \"/users/{id}\") void detail(@Path(\"userId\") String id, HttpListener<User> cb);")))
                .hadErrorContaining("{id} in the url has no @Path(\"id\")");
    }

    @Test
    public void bodyOnGet_isABuildError() {
        assertThat(compile(badRepository("@Get(url = \"/users\") void create(@Body User user, HttpListener<User> cb);")))
                .hadErrorContaining("a @Get request sends no body");
    }

    @Test
    public void returningAValue_isABuildError() {
        assertThat(compile(badRepository("@Get(url = \"/users\") User detail(HttpListener<User> cb);")))
                .hadErrorContaining("returns void");
    }

    @Test
    public void noVerb_orWildcardBody_areBuildErrors() {
        assertThat(compile(badRepository("void detail(HttpListener<User> cb);"))).hadErrorContaining("needs @Get, @Post");
        assertThat(compile(badRepository("@Get(url = \"/users\") void list(HttpListener<List<? extends User>> cb);")))
                .hadErrorContaining("must be a concrete type");
    }

    // ------------------------------------------------------------------ screens

    @Test
    public void callbacks_styleA_withBodyResponseNothingAndAllSuccess() {
        Compilation compilation = compile(REPOSITORY, screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "import com.onevour.core.rest.models.*;",
                "import java.util.List;",
                "public class UserActivity {",
                "    final UserRepository repository = null;",
                "    void load() {",
                "        repository.detail(\"42\", \"t\", null, UserActivityCallbacks.detail(this));",
                "        repository.search(\"budi\", 1, UserActivityCallbacks.search(this));",
                "        repository.remove(7L, UserActivityCallbacks.removed(this));",
                "    }",
                "    @OnSuccess(\"detail\") void onDetail(User user) { }",
                "    @OnError(\"detail\") void onDetailError(HttpErrorResponse error) { }",
                "    @OnSuccess(\"search\") void onSearch(HttpResponse<List<User>> response) { }",
                "    @OnSuccess(\"removed\") void onRemoved() { }",
                "    @OnAllSuccess({\"search\", \"detail\"}) void onReady() { }",
                "}"));
        assertThat(compilation).succeededWithoutWarnings();
        String generated = "app.UserActivityCallbacks";
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("static HttpListener<User> detail(UserActivity target)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("it.onDetail(response.getBody());");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("it.onSearch(response);");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("static <T> HttpListener<T> removed(UserActivity target)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("if (AllSuccess.succeeded(it, \"detail+search\", \"detail\", \"detail\", \"search\")) it.onReady();");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("it.onDetailError(error);");
    }

    @Test
    public void api_styleB_callsTheRepositoryWithTheCallbacks() {
        Compilation compilation = compile(REPOSITORY, screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "import com.onevour.core.rest.models.*;",
                "import java.util.List;",
                "@RestCallbacks(UserRepository.class)",
                "public class UserActivity {",
                "    final UserActivityApi api = new UserActivityApi(this);",
                "    void load() { api.detail(\"42\", \"t\", null); api.search(\"budi\", 1); api.create(new User()); api.remove(7L); }",
                "    @OnSuccess(\"detail\") void onDetail(User user) { }",
                "    @OnSuccess(\"search\") void onSearch(List<User> users) { }",
                "    @OnSuccess(\"create\") void onCreate(HttpResponse<User> response) { }",
                "    @OnSuccess(\"remove\") void onRemove(String text) { }",
                "}"));
        assertThat(compilation).succeededWithoutWarnings();
        String generated = "app.UserActivityApi";
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("public void detail(String id, String token, HttpHeaders extra)");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("userRepository.detail(id, token, extra, UserActivityCallbacks.detail(screen));");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("this(screen, new RestClient().create(UserRepository.class));");
        assertThat(compilation).generatedSourceFile(generated).contentsAsUtf8String().contains("public UserActivityApi(UserActivity screen, UserRepository userRepository)");
    }

    @Test
    public void api_withoutOnSuccessForAMethod_warnsAndOnlyLogs() {
        Compilation compilation = compile(REPOSITORY, screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "@RestCallbacks(UserRepository.class)",
                "public class UserActivity {",
                "    @OnSuccess(\"detail\") void onDetail(User user) { }",
                "}"));
        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("no @OnSuccess(\"search\")");
        assertThat(compilation).generatedSourceFile("app.UserActivityApi").contentsAsUtf8String()
                .contains("userRepository.search(name, page, new TypedHttpListener<>(RestTypes.of(List.class, User.class), null, null));");
    }

    @Test
    public void api_wrongBodyType_isABuildError() {
        assertThat(compile(REPOSITORY, screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "@RestCallbacks(UserRepository.class)",
                "public class UserActivity {",
                "    @OnSuccess(\"search\") void onSearch(User user) { }",
                "}"))).hadErrorContaining("@OnSuccess(\"search\") receives app.User but UserRepository.search() answers java.util.List<app.User>");
    }

    @Test
    public void onErrorWithoutOnSuccess_andDuplicates_areBuildErrors() {
        assertThat(compile(screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "public class UserActivity {",
                "    @OnError(\"detail\") void onDetailError() { }",
                "}"))).hadErrorContaining("@OnError(\"detail\") without an @OnSuccess(\"detail\")");
        assertThat(compile(screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "public class UserActivity {",
                "    @OnSuccess(\"detail\") void a(User u) { }",
                "    @OnSuccess(\"detail\") void b(User u) { }",
                "}"))).hadErrorContaining("two @OnSuccess(\"detail\")");
    }

    @Test
    public void restCallbacks_ofANonRepository_isABuildError() {
        assertThat(compile(screen(
                "package app;",
                "import com.onevour.core.rest.annotations.*;",
                "@RestCallbacks(User.class)",
                "public class UserActivity { }"))).hadErrorContaining("is not a @RestRepository interface");
    }
}
