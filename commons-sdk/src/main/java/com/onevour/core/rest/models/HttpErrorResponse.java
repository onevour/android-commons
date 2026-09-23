/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onevour.core.rest.models;

import com.onevour.core.rest.constants.HttpStatusCode;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * @author zuliadin
 */
public class HttpErrorResponse {

    private int code = 0;

    private String message;

    private String error;

    private Exception exception;

    public HttpErrorResponse(int code) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
    }

    public HttpErrorResponse(int code, String error) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        this.error = error;
    }

    public int getCode() {
        return code;
    }


    public HttpErrorResponse(int code, MalformedURLException malformedURLException) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        exception = malformedURLException;
        error = exception.getMessage();
    }

    public HttpErrorResponse(int code, String endpoint, IOException iOException) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        exception = iOException;
        error = exception.getMessage();
    }

    public HttpErrorResponse(int code, IOException iOException) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        exception = iOException;
        error = exception.getMessage();
    }

    public HttpErrorResponse(int code, IOException iOException, String message) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        exception = iOException;
        error = exception.getMessage();
    }

    public HttpErrorResponse(int code, Exception exception) {
        this.code = code;
        message = HttpStatusCode.getMessage(code);
        this.exception = exception;
        error = exception.getMessage();
    }


    public Exception getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

}
