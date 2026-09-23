package com.onevour.core.rest;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.repository.RestRepository;

@RestRepository
public interface UserRepository {

    @Post(key = "http://localhost:3000", url = "/users")
    void create(@Body UserRequest request, HttpListener<UserResponse> callback);

    @Post(key = "http://localhost:3000", url = "/users/{id}")
    void update(@Path(value = "id") String id, @Body UserRequest request, HttpListener<UserResponse> callback);
}
