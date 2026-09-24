package com.onevour.core;

import static org.junit.Assert.assertEquals;
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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public class ConcurrencyTest {

    private static final int REQUEST_COUNT = 16;

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
    public void shouldHandleMultipleRequestsConcurrently() throws Exception {
        for (int i = 0; i < REQUEST_COUNT; i++) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":" + i + ",\"name\":\"User" + i + "\"}"));
        }

        CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < REQUEST_COUNT; i++) {
            final int index = i;

            new Thread(() -> repository.create(new UserRequest(),
                    new HttpListener<UserResponse>() {
                        @Override
                        public void onSuccess(HttpResponse<UserResponse> response) {
                            try {
                                ids.add(response.getBody().getId());
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void onError(HttpErrorResponse error) {
                            latch.countDown();
                        }
                    }
            )).start();
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertEquals(REQUEST_COUNT, ids.size());
    }
}
