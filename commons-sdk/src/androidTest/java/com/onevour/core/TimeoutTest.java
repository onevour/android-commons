package com.onevour.core;

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

@RunWith(AndroidJUnit4.class)
public class TimeoutTest {

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
    public void readTimeout_shouldReturnError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1,\"name\":\"John\"}")
                .setBodyDelay(5, TimeUnit.SECONDS));

        CountDownLatch latch = new CountDownLatch(1);

        repository.getWithTimeout(new HttpListener<UserResponse>() {
            @Override
            public void onSuccess(HttpResponse<UserResponse> response) {
                latch.countDown();
            }

            @Override
            public void onError(HttpErrorResponse error) {
                assertNotNull(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}
