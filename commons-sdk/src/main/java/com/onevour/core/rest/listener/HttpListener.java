package com.onevour.core.rest.listener;

import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;

public interface HttpListener<T> {

    void onSuccess(HttpResponse<T> response);

    void onError(HttpErrorResponse httpErrorResponse);

}
