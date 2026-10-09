package com.onevour.sdk.impl.modules.rest;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.rest.RestInspector;
import com.onevour.core.rest.annotations.OnAllSuccess;
import com.onevour.core.rest.annotations.OnError;
import com.onevour.core.rest.annotations.OnSuccess;
import com.onevour.core.rest.annotations.RestCallbacks;
import com.onevour.core.rest.builder.GeneratedRepository;
import com.onevour.core.rest.builder.RestClient;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;
import com.onevour.sdk.impl.databinding.ActivityRestSampleBinding;
import com.onevour.sdk.impl.modules.location.SampleScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * android-commons REST against PokéAPI (GET), JSONPlaceholder (GET, POST, PUT, PATCH, DELETE) and
 * httpbin (HTTP errors, a timeout), every way to get an answer:
 * <ul>
 *     <li>style B: @RestCallbacks + the generated RestSampleActivityApi, answered by @OnSuccess /
 *     @OnError of the repository method's name, @OnAllSuccess once both calls are back;</li>
 *     <li>style A: RestSampleActivityCallbacks.firstPage(this), a name of its own;</li>
 *     <li>HttpListener.of with lambdas, and the anonymous HttpListener of before.</li>
 * </ul>
 * Each button opens RestExchangeDialog, fed by RestInspector: the request on top, the response below.
 */
@RestCallbacks({PokeRepository.class, PlaceholderRepository.class, HttpbinRepository.class})
public class RestSampleActivity extends AppCompatActivity {

    /** Style B: the repository's methods without a listener. */
    private final RestSampleActivityApi api = new RestSampleActivityApi(this);

    /** Styles A, lambda and before: the repository itself. */
    private final PokeRepository repository = new RestClient().create(PokeRepository.class);

    private ActivityRestSampleBinding binding;

    /** The open detail: RestInspector's observer, where the callbacks write. */
    private RestExchangeDialog detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestSampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SampleScreen.setUp(this, binding.toolbar, binding.content, "REST (PokéAPI)");
        binding.repositoryKind.setText(repository instanceof GeneratedRepository
                ? "Repository: kode hasil generate (" + repository.getClass().getSimpleName() + "), tanpa Proxy"
                : "Repository: Proxy (commons-sdk-processor tidak terpasang)");

        // style B: two calls together, answered by @OnSuccess("pokemon") / ("species"), then @OnAllSuccess
        binding.styleB.setOnClickListener(v -> {
            String name = name();
            open("Gaya B: data + spesies", "api.pokemon(\"" + name + "\")\napi.species(\"" + name + "\")", () -> {
                api.pokemon(name);
                api.species(name);
            });
        });
        // style A: a listener named here, answered by @OnSuccess("firstPage")
        binding.styleA.setOnClickListener(v -> open("Gaya A: daftar 5 pertama",
                "repository.list(5, 0, Callbacks.firstPage(this))",
                () -> repository.list(5, 0, RestSampleActivityCallbacks.firstPage(this))));
        // lambdas
        binding.lambda.setOnClickListener(v -> open("Lambda: tipe electric",
                "repository.type(\"electric\", HttpListener.of(...))",
                () -> repository.type("electric", HttpListener.of(
                        response -> result("lambda onSuccess: tipe " + response.getBody().name + ", " + response.getBody().pokemon.size()
                                + " Pokémon, mis. " + firstNames(response.getBody().pokemon)),
                        error -> result("lambda onError: " + describe(error))))));
        // before: an anonymous listener
        binding.anonymous.setOnClickListener(v -> open("Cara lama: kelas anonim",
                "repository.pokemon(\"bulbasaur\", new HttpListener<Pokemon>() {...})",
                () -> repository.pokemon("bulbasaur", new HttpListener<Poke.Pokemon>() {
                    @Override
                    public void onSuccess(HttpResponse<Poke.Pokemon> response) {
                        result("onSuccess: #" + response.getBody().id + " " + response.getBody().name + " (HTTP " + response.getCode() + ")");
                    }

                    @Override
                    public void onError(HttpErrorResponse error) {
                        result("onError: " + describe(error));
                    }
                })));
        // a name PokéAPI does not know: 404 to @OnError("pokemon")
        binding.notFound.setOnClickListener(v -> open("Error: nama tidak ada",
                "api.pokemon(\"bukan-pokemon\")", () -> api.pokemon("bukan-pokemon")));
        // JSONPlaceholder: every verb, style B
        binding.crudGet.setOnClickListener(v -> open("GET: list + detail + komentar",
                "api.posts(1)\napi.post(1)\napi.comments(1)", () -> {
                    api.posts(1);
                    api.post(1);
                    api.comments(1);
                }));
        binding.crudPost.setOnClickListener(v -> open("POST: buat post",
                "api.createPost(new Post(...), \"android-commons\")",
                () -> api.createPost(new Placeholder.Post(null, 1, "Halo dari android-commons", "dibuat lewat @Post"), "android-commons")));
        binding.crudPut.setOnClickListener(v -> open("PUT: ganti post",
                "api.updatePost(1, new Post(1, 1, ...))",
                () -> api.updatePost(1, new Placeholder.Post(1, 1, "Judul baru", "diganti semua lewat @Put"))));
        binding.crudPatch.setOnClickListener(v -> open("PATCH: ubah judul",
                "api.patchPost(1, new PostTitle(...))",
                () -> api.patchPost(1, new Placeholder.PostTitle("Judul diubah lewat @Patch"))));
        binding.crudDelete.setOnClickListener(v -> open("DELETE: hapus post",
                "api.deletePost(1)", () -> api.deletePost(1)));
        // httpbin: HTTP errors and a timeout, all to @OnError
        int[] codes = {400, 401, 404, 500, 503};
        Button[] statusButtons = {binding.error400, binding.error401, binding.error404, binding.error500, binding.error503};
        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];
            statusButtons[i].setOnClickListener(v -> open("Error HTTP " + code,
                    "api.status(" + code + ")", () -> api.status(code)));
        }
        binding.error422.setOnClickListener(v -> open("Error POST 422",
                "api.postStatus(422, new PostTitle(...))",
                () -> api.postStatus(422, new Placeholder.PostTitle("data tidak valid"))));
        binding.errorTimeout.setOnClickListener(v -> open("Timeout",
                "api.slow(8)  // jawaban 8 dtk, batas baca 4,5 dtk", () -> api.slow(8)));
    }

    @Override
    protected void onDestroy() {
        RestInspector.set(null);
        if (Objects.nonNull(detail)) detail.dismiss();
        super.onDestroy();
    }

    /** Opens the detail, watching the requests, then sends: the dialog sees each one go out. */
    private void open(String title, String call, Runnable send) {
        if (Objects.nonNull(detail)) detail.dismiss();
        RestExchangeDialog dialog = new RestExchangeDialog(this, title, call);
        dialog.setOnDismissListener(closed -> {
            if (detail != closed) return;                       // an older one, replaced already
            RestInspector.set(null);
            detail = null;
        });
        detail = dialog;
        RestInspector.set(dialog);
        dialog.show();
        send.run();
    }

    @OnSuccess("pokemon")
    void onPokemon(Poke.Pokemon pokemon) {
        List<String> types = new ArrayList<>();
        for (Poke.TypeSlot slot : pokemon.types) types.add(slot.type.name);
        result("@OnSuccess pokemon: #" + pokemon.id + " " + pokemon.name + ", tipe " + types
                + ", tinggi " + pokemon.height / 10.0 + " m, berat " + pokemon.weight / 10.0 + " kg");
    }

    @OnError("pokemon")
    void onPokemonFailed(HttpErrorResponse error) {
        result("@OnError pokemon: HTTP " + error.getCode() + (error.getCode() == 404 ? " (nama tidak ditemukan)" : ""));
    }

    @OnSuccess("species")
    void onSpecies(HttpResponse<Poke.Species> response) {
        Poke.Species species = response.getBody();
        result("@OnSuccess species: warna " + species.color.name + ", " + species.generation.name + " (HTTP " + response.getCode() + ")");
    }

    @OnError("species")
    void onSpeciesFailed(HttpErrorResponse error) {
        result("@OnError species: HTTP " + error.getCode());
    }

    @OnAllSuccess({"pokemon", "species"})
    void onPokemonReady() {
        result("@OnAllSuccess: data dan spesies lengkap");
    }

    @OnSuccess("firstPage")
    void onFirstPage(Poke.Page page) {
        result("@OnSuccess firstPage: " + page.count + " Pokémon, 5 pertama " + names(page.results));
    }

    @OnError("firstPage")
    void onFirstPageFailed(HttpErrorResponse error) {
        result("@OnError firstPage: HTTP " + error.getCode());
    }

    // ---- JSONPlaceholder ----

    @OnSuccess("posts")
    void onPosts(List<Placeholder.Post> posts) {
        result("@OnSuccess posts (GET + @Query): " + posts.size() + " post milik user 1");
    }

    @OnSuccess("post")
    void onPost(Placeholder.Post post) {
        result("@OnSuccess post (GET + @Path): #" + post.id + " \"" + shorten(post.title) + "\"");
    }

    @OnSuccess("comments")
    void onComments(List<Placeholder.Comment> comments) {
        result("@OnSuccess comments: " + comments.size() + " komentar, dari " + comments.get(0).email);
    }

    @OnAllSuccess({"post", "comments"})
    void onPostWithComments() {
        result("@OnAllSuccess: post dan komentarnya lengkap");
    }

    @OnSuccess("createPost")
    void onCreated(HttpResponse<Placeholder.Post> response) {
        result("@OnSuccess createPost (POST + @Body + @Header): HTTP " + response.getCode() + ", id baru " + response.getBody().id);
    }

    @OnSuccess("updatePost")
    void onUpdated(Placeholder.Post post) {
        result("@OnSuccess updatePost (PUT): #" + post.id + " \"" + post.title + "\"");
    }

    @OnSuccess("patchPost")
    void onPatched(Placeholder.Post post) {
        result("@OnSuccess patchPost (PATCH): #" + post.id + " \"" + post.title + "\"");
    }

    @OnSuccess("deletePost")
    void onDeleted(HttpResponse<Placeholder.Post> response) {
        result("@OnSuccess deletePost (DELETE): HTTP " + response.getCode());
    }

    @OnError("createPost")
    void onCreateFailed(HttpErrorResponse error) {
        result("@OnError createPost: HTTP " + error.getCode());
    }

    // ---- httpbin: errors ----

    @OnSuccess("status")
    void onStatus(HttpResponse<String> response) {
        result("@OnSuccess status: HTTP " + response.getCode());
    }

    @OnError("status")
    void onStatusFailed(HttpErrorResponse error) {
        result("@OnError status: " + describe(error));
    }

    @OnSuccess("postStatus")
    void onPostStatus(HttpResponse<String> response) {
        result("@OnSuccess postStatus: HTTP " + response.getCode());
    }

    @OnError("postStatus")
    void onPostStatusFailed(HttpErrorResponse error) {
        result("@OnError postStatus (POST): " + describe(error));
    }

    @OnSuccess("slow")
    void onSlow(String body) {
        result("@OnSuccess slow: tidak timeout?");
    }

    @OnError("slow")
    void onSlowFailed(HttpErrorResponse error) {
        result("@OnError slow: " + describe(error));
    }

    /** What an HttpErrorResponse tells: the status, or the exception when the request never got one. */
    private static String describe(HttpErrorResponse error) {
        String meaning;
        switch (error.getCode()) {
            case 400: meaning = "permintaan salah"; break;
            case 401: meaning = "belum login / token salah"; break;
            case 403: meaning = "tidak boleh"; break;
            case 404: meaning = "tidak ditemukan"; break;
            case 422: meaning = "data tidak valid"; break;
            case 500: meaning = "server error"; break;
            case 503: meaning = "server sedang tidak tersedia"; break;
            case 0: meaning = "tidak ada jawaban"; break;
            default: meaning = "error"; break;
        }
        String detail = error.getException() == null ? "" : ", " + error.getException().getClass().getSimpleName()
                + (error.getException().getMessage() == null ? "" : ": " + error.getException().getMessage());
        return "HTTP " + error.getCode() + " (" + meaning + ")" + detail;
    }

    private static String shorten(String text) {
        return text.length() > 30 ? text.substring(0, 30) + "..." : text;
    }

    private String name() {
        String name = String.valueOf(binding.name.getText()).trim().toLowerCase(Locale.ROOT);
        return name.isEmpty() ? "pikachu" : name;
    }

    private static List<String> names(List<Poke.Named> named) {
        List<String> result = new ArrayList<>();
        for (Poke.Named item : named) result.add(item.name);
        return result;
    }

    private static List<String> firstNames(List<Poke.TypePokemon> pokemon) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < pokemon.size() && i < 3; i++) result.add(pokemon.get(i).pokemon.name);
        return result;
    }

    /** A callback that ran, shown in the open detail (a late answer after it closed is dropped). */
    private void result(String line) {
        if (Objects.nonNull(detail)) detail.callback(line);
    }
}
