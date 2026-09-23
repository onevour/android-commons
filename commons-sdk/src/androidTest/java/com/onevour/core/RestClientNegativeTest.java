package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.onevour.core.rest.UserRepository;
import com.onevour.core.rest.UserRequest;
import com.onevour.core.rest.UserResponse;
import com.onevour.core.rest.builder.RestClient;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class RestClientNegativeTest {

    private MockWebServer mockWebServer;
    private UserRepository repository;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(3000);
        repository = new RestClient().create(UserRepository.class);
    }

    @After
    public void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    public void shouldHandle400BadRequest() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(400)
                        .setBody("{\"message\":\"Bad request\"}")
        );

        final CountDownLatch latch = new CountDownLatch(1);

        final AtomicReference<UserResponse> response =
                new AtomicReference<>();

        final AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest("Budi"),
                new HttpListener<>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {

                        fail("Expected onError(), but onSuccess() was called");
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        assertNotNull(
                "Expected error",
                error.get()
        );

        assertTrue(error.get() instanceof HttpErrorResponse);

        HttpErrorResponse httpError = (HttpErrorResponse) error.get();

        assertEquals(
                400,
                httpError.getCode()
        );

        RecordedRequest request =
                mockWebServer.takeRequest();

        assertEquals(
                "POST",
                request.getMethod()
        );

        assertEquals(
                "/users",
                request.getRequestUrl().encodedPath()
        );
    }

    @Test
    public void shouldHandle401Unauthorized() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(401)
                        .setBody("{\"message\":\"Unauthorized\"}")
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest(),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(
                            HttpResponse<UserResponse> response) {

                        fail("Expected onError()");
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        assertNotNull(error.get());

        HttpErrorResponse httpError =
                (HttpErrorResponse) error.get();

        assertEquals(
                401,
                httpError.getCode()
        );
    }

    @Test
    public void shouldHandle404NotFound() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setBody("{\"message\":\"Not found\"}")
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest(),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(
                            HttpResponse<UserResponse> response) {

                        fail("Expected onError()");
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        assertNotNull(error.get());

        HttpErrorResponse httpError = (HttpErrorResponse) error.get();

        assertEquals(
                404,
                httpError.getCode()
        );
    }

    @Test
    public void shouldHandle500InternalServerError() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(500)
                        .setBody("{\"message\":\"Internal server error\"}")
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest(),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(
                            HttpResponse<UserResponse> response) {

                        fail("Expected onError()");
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        assertNotNull(error.get());

        HttpErrorResponse httpError = (HttpErrorResponse) error.get();

        assertEquals(
                500,
                httpError.getCode()
        );
    }

    @Test
    public void shouldHandleInvalidJsonResponse() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("{ invalid json")
        );

        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<UserResponse> response = new AtomicReference<>();

        AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest(),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> httpResponse) {
                        response.set(httpResponse.getBody());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        /*
         * Invalid JSON seharusnya tidak membuat test
         * menggantung. Callback harus tetap terjadi.
         */
        assertTrue(
                response.get() == null || error.get() != null
        );
    }

    @Test
    public void shouldHandleConnectionFailure() throws Exception {

        // Shutdown server sehingga connection akan gagal.
        mockWebServer.shutdown();

        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<HttpErrorResponse> error = new AtomicReference<>();

        repository.create(
                new UserRequest(),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(
                            HttpResponse<UserResponse> response) {

                        fail("Expected onError()");
                    }

                    @Override
                    public void onError(HttpErrorResponse throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }
        );

        assertTrue(
                "API callback timeout",
                latch.await(10, TimeUnit.SECONDS)
        );

        assertNotNull(
                "Expected connection error",
                error.get()
        );
    }
}