package com.onevour.core.rest;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.repository.RestRepository;

@RestRepository
public interface UserRepository {

    @Post(url = "http://localhost:3000/users")
    void create(@Body UserRequest request, HttpListener<UserResponse> callback);

    @Post(url = "http://localhost:3000/users/{id}")
    void update(@Path(value = "id") String id, @Body UserRequest request, HttpListener<UserResponse> callback);
}
