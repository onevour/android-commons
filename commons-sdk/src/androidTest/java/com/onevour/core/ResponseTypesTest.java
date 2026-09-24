package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.onevour.core.rest.UserRepository;
import com.onevour.core.rest.UserResponse;
import com.onevour.core.rest.builder.RestClient;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public class ResponseTypesTest {

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
        server.shutdown();
    }

    @Test
    public void shouldParseObjectResponse() throws Exception {
        server.enqueue(json(200, "{\"id\":1,\"name\":\"John\"}"));

        CountDownLatch latch = new CountDownLatch(1);

        repository.getObject(new HttpListener<UserResponse>() {
            @Override
            public void onSuccess(HttpResponse<UserResponse> response) {
                try {
                    assertEquals(1, response.getBody().getId());
                    assertEquals("John", response.getBody().getName());
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(HttpErrorResponse error) {
                latch.countDown();
                throw new AssertionError("Unexpected HTTP " + error.getCode());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldParseStringResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("hello world"));

        CountDownLatch latch = new CountDownLatch(1);

        repository.getString(new HttpListener<>() {
            @Override
            public void onSuccess(HttpResponse<String> response) {
                try {
                    assertEquals("hello world", response.getBody());
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(HttpErrorResponse error) {
                latch.countDown();
                throw new AssertionError("Unexpected HTTP " + error.getCode());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldParseListResponse() throws Exception {
        server.enqueue(json(200,
                "[{\"id\":1,\"name\":\"John\"},{\"id\":2,\"name\":\"Jane\"}]"));

        CountDownLatch latch = new CountDownLatch(1);

        repository.getList(new HttpListener<List<UserResponse>>() {
            @Override
            public void onSuccess(HttpResponse<List<UserResponse>> response) {
                try {
                    assertNotNull(response.getBody());
                    assertEquals(2, response.getBody().size());
                    assertEquals("John", response.getBody().get(0).getName());
                    assertEquals("Jane", response.getBody().get(1).getName());
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(HttpErrorResponse error) {
                latch.countDown();
                throw new AssertionError("Unexpected HTTP " + error.getCode());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private MockResponse json(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
