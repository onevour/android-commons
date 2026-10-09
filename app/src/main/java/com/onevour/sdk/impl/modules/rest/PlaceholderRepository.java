package com.onevour.sdk.impl.modules.rest;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Delete;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Header;
import com.onevour.core.rest.annotations.Patch;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.annotations.Put;
import com.onevour.core.rest.annotations.Query;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.repository.RestRepository;

import java.util.List;

/** JSONPlaceholder: every HTTP verb of the REST client (nothing is really stored there). */
@RestRepository
public interface PlaceholderRepository {

    String BASE = "https://jsonplaceholder.typicode.com";

    @Get(key = BASE, url = "/posts")
    void posts(@Query("userId") int userId, HttpListener<List<Placeholder.Post>> callback);

    @Get(key = BASE, url = "/posts/{id}")
    void post(@Path("id") int id, HttpListener<Placeholder.Post> callback);

    @Get(key = BASE, url = "/posts/{id}/comments")
    void comments(@Path("id") int id, HttpListener<List<Placeholder.Comment>> callback);

    @Post(key = BASE, url = "/posts")
    void createPost(@Body Placeholder.Post post, @Header("X-Sample") String sample, HttpListener<Placeholder.Post> callback);

    @Put(key = BASE, url = "/posts/{id}")
    void updatePost(@Path("id") int id, @Body Placeholder.Post post, HttpListener<Placeholder.Post> callback);

    @Patch(key = BASE, url = "/posts/{id}")
    void patchPost(@Path("id") int id, @Body Placeholder.PostTitle title, HttpListener<Placeholder.Post> callback);

    @Delete(key = BASE, url = "/posts/{id}")
    void deletePost(@Path("id") int id, HttpListener<Placeholder.Post> callback);
}
