package com.onevour.sdk.impl.modules.rest;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.repository.RestRepository;

/** httpbin.org: answers with the status asked for, or late, to show every kind of failure. */
@RestRepository
public interface HttpbinRepository {

    String BASE = "https://httpbin.org";

    @Get(key = BASE, url = "/status/{code}")
    void status(@Path("code") int code, HttpListener<String> callback);

    @Post(key = BASE, url = "/status/{code}")
    void postStatus(@Path("code") int code, @Body Placeholder.PostTitle body, HttpListener<String> callback);

    /** Answers after {seconds}; the read timeout (1 s, at least 4.5 s in the client) runs out first. */
    @Get(key = BASE, url = "/delay/{seconds}", connect = 1, read = 1)
    void slow(@Path("seconds") int seconds, HttpListener<String> callback);
}
