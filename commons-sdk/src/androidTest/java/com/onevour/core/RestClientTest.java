package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class RestClientTest extends RestBaseTest {

    private static final String TAG = RestClientTest.class.getSimpleName();

    private MockWebServer server;


    @Before
    public void setUp() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(readAsset(context, "user-response.json")));
        server.start(3000);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void shouldCallApiCreate() throws Exception {

        UserRepository repository = client.create(UserRepository.class);

        CountDownLatch latch = new CountDownLatch(1);

        repository.create(
                new UserRequest("Budi"),
                new HttpListener<UserResponse>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        UserResponse body = response.getBody();
                        assertEquals(1, body.getId());
                        assertEquals("Budi", body.getName());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HttpErrorResponse httpErrorResponse) {
                        latch.countDown();
                    }

                }
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());

        assertEquals("/users", request.getPath());

        assertEquals("application/json", request.getHeader("Content-Type"));

        assertEquals("{\"name\":\"Budi\"}", request.getBody().readUtf8());
    }

    @Test
    public void shouldCallApiUpdate() throws Exception {

        UserRepository repository = client.create(UserRepository.class);

        CountDownLatch latch = new CountDownLatch(1);

        repository.updateWithPost("10", new UserRequest("Budi"), new HttpListener<>() {

                    @Override
                    public void onSuccess(HttpResponse<UserResponse> response) {
                        UserResponse body = response.getBody();

                        assertEquals(1, body.getId());
                        assertEquals("Budi", body.getName());

                        latch.countDown();
                    }

                    @Override
                    public void onError(HttpErrorResponse httpErrorResponse) {
                        Log.d(TAG, "http: " + httpErrorResponse.getCode() + " | " + httpErrorResponse.getMessage());
                        latch.countDown();
                    }
                }
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());

        assertEquals("/users/10", request.getPath());

        assertEquals("application/json", request.getHeader("Content-Type"));

        assertEquals("{\"name\":\"Budi\"}", request.getBody().readUtf8());
    }
}