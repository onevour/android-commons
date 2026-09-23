package com.onevour.core.rest;

import com.google.gson.annotations.Expose;

public class UserRequest {

    @Expose
    String name;

    public UserRequest() {
    }

    public UserRequest(String name) {
        this.name = name;
    }
}
