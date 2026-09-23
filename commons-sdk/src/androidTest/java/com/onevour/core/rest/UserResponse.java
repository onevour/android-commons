package com.onevour.core.rest;

import com.google.gson.annotations.Expose;

public class UserResponse {

    @Expose
    int id;

    @Expose
    String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
