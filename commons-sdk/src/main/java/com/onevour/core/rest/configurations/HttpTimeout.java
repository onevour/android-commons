package com.onevour.core.rest.configurations;

public class HttpTimeout {

    int connect = 0;


    int read = 0;

    public HttpTimeout() {
    }

    public HttpTimeout(int connect, int read) {
        this.connect = connect;
        this.read = read;
    }

    public int getConnect() {
        return connect;
    }


    public int getRead() {
        return read;
    }

    public void setValue(int connect, int read) {
        this.connect = connect;
        this.read = read;
    }
}
