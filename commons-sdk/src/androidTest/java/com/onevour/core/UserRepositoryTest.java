package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.onevour.core.rest.UserRepository;
import com.onevour.core.rest.UserRequest;
import com.onevour.core.rest.UserResponse;
import com.onevour.core.rest.builder.RestClient;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class UserRepositoryTest {

    private MockWebServer server;
    private UserRepository repository;


    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start(3000);

        repository = new RestClient().create(UserRepository.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void create_shouldSendPostWithJsonBody() throws Exception {

        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                "{"
                                        + "\"id\":1,"
                                        + "\"name\":\"John\""
                                        + "}"
                        )
        );

        CountDownLatch latch = new CountDownLatch(1);

        UserRequest request = new UserRequest();
        request.setName("John");

        repository.create(
                request,
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        try {
                            assertEquals(200, response.getCode());
                            assertNotNull(response.getBody());
                            assertEquals(1, response.getBody().getId());
                            assertEquals("John", response.getBody().getName());
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(HttpErrorResponse error) {
                        latch.countDown();
                        throw new AssertionError(
                                "Request should succeed but got error: "
                                        + error.getCode()
                        );
                    }
                }
        );

        RecordedRequest recordedRequest =
                server.takeRequest(5, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);

        assertEquals(
                "POST",
                recordedRequest.getMethod()
        );

        assertEquals(
                "/users",
                recordedRequest.getPath()
        );

        assertEquals(
                "application/json",
                recordedRequest.getHeader("Content-Type")
        );

        assertEquals(
                "{\"name\":\"John\"}",
                recordedRequest.getBody().readUtf8()
        );

        assertTrue(
                latch.await(5, TimeUnit.SECONDS)
        );
    }

    @Test
    public void update_shouldReplacePathVariable() throws Exception {

        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                "{"
                                        + "\"id\":123,"
                                        + "\"name\":\"Updated\""
                                        + "}"
                        )
        );

        CountDownLatch latch = new CountDownLatch(1);

        UserRequest request = new UserRequest();
        request.setName("Updated");

        repository.updateWithPost(
                "123",
                request,
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        try {
                            assertEquals(200, response.getCode());
                            assertNotNull(response.getBody());
                            assertEquals(123, response.getBody().getId());
                            assertEquals(
                                    "Updated",
                                    response.getBody().getName()
                            );
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(HttpErrorResponse error) {
                        latch.countDown();
                        throw new AssertionError(
                                "Request should succeed but got error: "
                                        + error.getCode()
                        );
                    }
                }
        );

        RecordedRequest recordedRequest =
                server.takeRequest(5, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);

        assertEquals(
                "POST",
                recordedRequest.getMethod()
        );

        assertEquals(
                "/users/123",
                recordedRequest.getPath()
        );

        assertTrue(
                latch.await(5, TimeUnit.SECONDS)
        );
    }

    @Test
    public void create_shouldReturnErrorResponse() throws Exception {

        server.enqueue(
                new MockResponse()
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
                        .setHeader("X-Request-Id", "abc123")
                        .setBody(
                                "{"
                                        + "\"message\":\"Unauthorized\""
                                        + "}"
                        )
        );

        CountDownLatch latch = new CountDownLatch(1);

        UserRequest request = new UserRequest();
        request.setName("John");

        repository.create(
                request,
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        latch.countDown();
                        throw new AssertionError(
                                "Request should fail"
                        );
                    }

                    @Override
                    public void onError(HttpErrorResponse error) {
                        try {
                            assertEquals(401, error.getCode());

                            assertNotNull(error.getHeaders());

                            assertEquals(
                                    "abc123",
                                    error.getHeaders()
                                            .get("X-Request-Id")
                            );
                        } finally {
                            latch.countDown();
                        }
                    }
                }
        );

        assertTrue(
                latch.await(5, TimeUnit.SECONDS)
        );
    }

    @Test
    public void create_shouldHandleNoContent() throws Exception {

        server.enqueue(
                new MockResponse()
                        .setResponseCode(204)
        );

        CountDownLatch latch = new CountDownLatch(1);

        UserRequest request = new UserRequest();
        request.setName("John");

        repository.create(
                request,
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        try {
                            assertEquals(204, response.getCode());
                            assertNull(response.getBody());
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(HttpErrorResponse error) {
                        latch.countDown();
                        throw new AssertionError(
                                "204 should be success"
                        );
                    }
                }
        );

        assertTrue(
                latch.await(5, TimeUnit.SECONDS)
        );
    }

}