package com.onevour.core.utilities.http;

import android.util.Log;

import com.onevour.core.utilities.commons.ValueOf;
import com.onevour.core.utilities.eventbus.MessageEvent;
import com.onevour.core.utilities.commons.RefSession;
import com.onevour.core.utilities.jwt.JWTCommons;
import com.onevour.core.utilities.jwt.JWTTokenRefreshRequest;
import com.onevour.core.utilities.jwt.JWTTokenRefreshResponse;
import org.greenrobot.eventbus.EventBus;

import java.util.Map;

/**
 * <b>API_TOKEN</b> : set after success login
 * <b>API_TOKEN_REFRESH</b> : set after success login</br>
 * <b>API_TOKEN_REFRESH_URL</b> : token refresh url, set on create application</br>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ApiRequestBuilder {

    private final String TAG = ApiRequestBuilder.class.getSimpleName();

    private final RefSession session = new RefSession();

    private Map<String, String> header;

    private String url;

    private String method;

    private Object body;

    private boolean validateToken = false;

    private HttpListener listener;

    public ApiRequestBuilder validateToken() {
        String refreshToken = session.findString("API_TOKEN");
        this.validateToken = JWTCommons.isExpired(refreshToken);
        Log.d(TAG, "token expired add new request " + validateToken);
        return this;
    }

    public ApiRequestBuilder get(String url) {
        this.method = "GET";
        this.url = url;
        this.header = null;
        return this;
    }

    public ApiRequestBuilder get(String url, Map<String, String> header) {
        this.method = "GET";
        this.url = url;
        this.header = header;
        return this;
    }

    public ApiRequestBuilder post(String url, Object body) {
        this.method = "POST";
        this.url = url;
        this.header = null;
        this.body = body;
        return this;
    }

    public ApiRequestBuilder post(String url, Map<String, String> header, Object body) {
        this.method = "POST";
        this.url = url;
        this.header = header;
        this.body = body;
        return this;
    }

    public ApiRequestBuilder put(String url, Object body) {
        this.method = "PUT";
        this.url = url;
        this.header = null;
        this.body = body;
        return this;
    }

    public ApiRequestBuilder put(String url, Map<String, String> header, Object body) {
        this.method = "PUT";
        this.url = url;
        this.header = header;
        this.body = body;
        return this;
    }

    public ApiRequestBuilder delete(String url, Object body) {
        this.method = "DELETE";
        this.url = url;
        this.header = null;
        this.body = body;
        return this;
    }

    public ApiRequestBuilder delete(String url, Map<String, String> header, Object body) {
        this.method = "DELETE";
        this.url = url;
        this.header = header;
        this.body = body;
        return this;
    }

    private void refreshToken() {
        String refreshTokenUrl = session.findString("API_TOKEN_REFRESH_URL");
        if (ValueOf.isEmpty(refreshTokenUrl)) return;
        String refreshToken = session.findString("API_TOKEN_REFRESH");
        JWTTokenRefreshRequest request = new JWTTokenRefreshRequest();
        request.setRefreshToken(refreshToken);
        Log.d(TAG, "request token refresh with id " + refreshToken);
        ApiRequest.post(refreshTokenUrl, request, new HttpListener<Response<JWTTokenRefreshResponse>>() {
            @Override
            public void onSuccess(Response<JWTTokenRefreshResponse> response) {
                Log.d(TAG, "request token refresh " + response.getCode() + " | " + response.getMessage());
                if (404 == response.getCode()) {
                    EventBus.getDefault().post(new MessageEvent("LOGOUT_EXPIRED"));
                    return;
                }
                if (200 == response.getCode()) {
                    JWTTokenRefreshResponse newToken = response.getResult();
                    if (ValueOf.isNull(newToken)) {
                        Log.e(TAG, "error getting new token");
                        error(404, "error getting new token");
                        return;
                    }
                    if (ValueOf.isEmpty(newToken.getAccessToken())) {
                        Log.e(TAG, "error getting new token access");
                        error(404, "error getting new token access");
                        return;
                    }
                    if (ValueOf.isEmpty(newToken.getRefreshToken())) {
                        Log.e(TAG, "error getting new token access refresh");
                        error(404, "error getting new token access refresh");
                        return;
                    }
                    Log.i(TAG, "new token receive");
                    session.saveString("API_TOKEN", newToken.getAccessToken());
                    session.saveString("API_TOKEN_REFRESH", newToken.getRefreshToken());
                    // update header
                    if (ValueOf.nonNull(header)) {
                        header.put("Authorization", "Bearer " + newToken.getAccessToken());
                    }
                    request();
                    return;
                }
                error(response.getCode(), response.getMessage());
            }

            @Override
            public void onError(Error error) {
                Log.e(TAG, "request token refresh " + error.getMessage());
                if (ValueOf.isNull(listener)) return;
                listener.onError(error);
            }
        });
    }

    private void error(int code, String message) {
        if (ValueOf.isNull(listener)) return;
        Error error = new Error(code);
        error.setMessage(message);
        listener.onError(error);
    }

    private void request() {
        if ("GET".equalsIgnoreCase(method)) {
            ApiRequest.get(url, header, listener);
        }
        if ("POST".equalsIgnoreCase(method)) {
            ApiRequest.post(url, header, body, listener);
        }
        if ("PUT".equalsIgnoreCase(method)) {
            ApiRequest.put(url, header, body, listener);
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            ApiRequest.delete(url, header, body, listener);
        }
    }

    public void execute() {
        execute(null);
    }

    public <T> void execute(HttpListener<T> listener) {
        this.listener = listener;
        if (validateToken) {
            refreshToken();
            return;
        }
        request();
    }
}
