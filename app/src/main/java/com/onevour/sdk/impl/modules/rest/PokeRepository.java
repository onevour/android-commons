package com.onevour.sdk.impl.modules.rest;

import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Query;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.repository.RestRepository;

/**
 * PokéAPI as an app declares a repository. commons-sdk-processor generates PokeRepository_Rest (no
 * Proxy, the body types taken from here) and checks every {name} against its @Path at build time.
 */
@RestRepository
public interface PokeRepository {

    String BASE = "https://pokeapi.co/api/v2";

    @Get(key = BASE, url = "/pokemon/{name}")
    void pokemon(@Path("name") String name, HttpListener<Poke.Pokemon> callback);

    @Get(key = BASE, url = "/pokemon-species/{name}")
    void species(@Path("name") String name, HttpListener<Poke.Species> callback);

    @Get(key = BASE, url = "/pokemon")
    void list(@Query("limit") int limit, @Query("offset") int offset, HttpListener<Poke.Page> callback);

    @Get(key = BASE, url = "/type/{type}")
    void type(@Path("type") String type, HttpListener<Poke.Type> callback);
}
