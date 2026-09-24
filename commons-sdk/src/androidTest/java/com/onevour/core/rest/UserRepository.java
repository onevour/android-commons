package com.onevour.core.rest;

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

@RestRepository
public interface UserRepository {

    @Get(url = "http://localhost:3000/users")
    void getUser(
            @Query("name") String name,
            HttpListener<UserResponse> callback
    );

    @Get(url = "http://localhost:3000/users")
    void searchUser(
            @Query("name") String name,
            @Query("page") int page,
            HttpListener<UserResponse> callback
    );

    @Post(url = "http://localhost:3000/users")
    public void create(
            @Body UserRequest request,
            HttpListener<UserResponse> callback
    );

    @Put(url = "http://localhost:3000/users/{id}")
    public void update(
            @Path("id") String id,
            @Body UserRequest request,
            HttpListener<UserResponse> callback
    );

    @Post(url = "http://localhost:3000/users/{id}")
    public void updateWithPost(
            @Path("id") String id,
            @Body UserRequest request,
            HttpListener<UserResponse> callback
    );

    @Patch(url = "http://localhost:3000/users/{id}")
    public void patch(
            @Path("id") String id,
            @Body UserRequest request,
            HttpListener<UserResponse> callback
    );

    @Delete(url = "http://localhost:3000/users/{id}")
    public void delete(
            @Path("id") String id,
            HttpListener<UserResponse> callback
    );

    @Get(
            url = "http://localhost:3000/users",
            connect = 1,
            read = 1
    )
    void getWithTimeout(
            HttpListener<UserResponse> callback
    );

    @Get(url = "http://localhost:3000/users")
    void getWithHeaders(
            @Query("name") String name,
            @Header("Authorization") String authorization,
            HttpListener<UserResponse> callback
    );

    @Get(url = "http://localhost:3000/users/1")
    void getObject(
            HttpListener<UserResponse> callback
    );

    @Get(url = "http://localhost:3000/users/raw")
    void getString(
            HttpListener<String> callback
    );

    @Get(url = "http://localhost:3000/users")
    void getList(
            HttpListener<List<UserResponse>> callback
    );
}
