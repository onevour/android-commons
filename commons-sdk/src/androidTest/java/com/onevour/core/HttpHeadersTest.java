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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class HttpHeadersTest {

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
    public void shouldSendRequestHeadersAndReceiveResponseHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Request-Id", "req-123")
                .setHeader("X-Test", "hello")
                .setBody("{\"id\":1,\"name\":\"John\"}"));

        CountDownLatch latch = new CountDownLatch(1);

        repository.getWithHeaders("John", "Bearer token-123", new HttpListener<UserResponse>() {
            @Override
            public void onSuccess(HttpResponse<UserResponse> response) {
                try {
                    assertEquals(200, response.getCode());
                    assertNotNull(response.getHeaders());
                    assertEquals("req-123", response.getHeaders().get("X-Request-Id"));
                    assertEquals("hello", response.getHeaders().get("X-Test"));
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

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);

        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/users?name=John", request.getPath());
        assertEquals("Bearer token-123", request.getHeader("Authorization"));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
