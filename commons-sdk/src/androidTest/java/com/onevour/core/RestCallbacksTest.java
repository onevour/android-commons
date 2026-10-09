package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.onevour.core.rest.UserRepository;
import com.onevour.core.rest.UserResponse;
import com.onevour.core.rest.annotations.OnAllSuccess;
import com.onevour.core.rest.annotations.OnError;
import com.onevour.core.rest.annotations.OnSuccess;
import com.onevour.core.rest.annotations.RestCallbacks;
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** @OnSuccess / @OnError / @OnAllSuccess, called through the generated Api (style B) and Callbacks (style A), and HttpListener.of. */
@RunWith(AndroidJUnit4.class)
public class RestCallbacksTest {

    @RestCallbacks(UserRepository.class)
    public static class Screen {
        final CountDownLatch done = new CountDownLatch(2);
        UserResponse user;
        List<UserResponse> users;
        HttpErrorResponse error;
        int ready;

        @OnSuccess("getObject")
        void onUser(UserResponse user) {
            this.user = user;
            done.countDown();
        }

        @OnError("getObject")
        void onUserFailed(HttpErrorResponse error) {
            this.error = error;
            done.countDown();
        }

        @OnSuccess("getList")
        void onUsers(HttpResponse<List<UserResponse>> response) {
            this.users = response.getBody();
            done.countDown();
        }

        @OnAllSuccess({"getObject", "getList"})
        void onReady() {
            ready++;
        }

        /** Style A: a name of its own, used with RestCallbacksTest_ScreenCallbacks.mine(this). */
        @OnSuccess("mine")
        void onMine(UserResponse user) {
            this.user = user;
            done.countDown();
            done.countDown();
        }
    }

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start(3000);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private void json(int code, String body) {
        server.enqueue(new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body));
    }

    /** Requests sent together arrive in any order: answer by path, not by queue. */
    private void byPath(final MockResponse user, final MockResponse users) {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return "/users/1".equals(request.getPath()) ? user : users;
            }
        });
    }

    private static MockResponse response(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    public void api_parsesEachBody_andCallsAllSuccessOnce() throws Exception {
        byPath(response(200, "{\"id\":1,\"name\":\"Budi\"}"),
                response(200, "[{\"id\":1,\"name\":\"Budi\"},{\"id\":2,\"name\":\"Sari\"}]"));
        Screen screen = new Screen();
        RestCallbacksTest_ScreenApi api = new RestCallbacksTest_ScreenApi(screen);
        api.getObject();
        api.getList();
        assertTrue(screen.done.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertEquals("Budi", screen.user.getName());
        assertEquals(2, screen.users.size());
        assertEquals("Sari", screen.users.get(1).getName());
        assertEquals(1, screen.ready);
        assertNull(screen.error);
    }

    @Test
    public void api_failure_goesToOnError_andNotToAllSuccess() throws Exception {
        byPath(response(500, "{}"), response(200, "[]"));
        Screen screen = new Screen();
        RestCallbacksTest_ScreenApi api = new RestCallbacksTest_ScreenApi(screen);
        api.getObject();
        api.getList();
        assertTrue(screen.done.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertEquals(500, screen.error.getCode());
        assertNull(screen.user);
        assertEquals(0, screen.ready);
    }

    @Test
    public void callbacks_styleA_andTheProxyFallback_parseWithTheCallbacksType() throws Exception {
        json(200, "{\"id\":3,\"name\":\"Andi\"}");
        Screen screen = new Screen();
        UserRepository proxy = new RestClient(false).create(UserRepository.class);   // no generated code: the type comes from @OnSuccess
        proxy.getObject(RestCallbacksTest_ScreenCallbacks.mine(screen));
        assertTrue(screen.done.await(5, TimeUnit.SECONDS));
        assertEquals("Andi", screen.user.getName());
    }

    @Test
    public void httpListenerOf_withAType_worksWithTheProxyToo() throws Exception {
        json(200, "{\"id\":4,\"name\":\"Rina\"}");
        CountDownLatch latch = new CountDownLatch(1);
        final UserResponse[] got = new UserResponse[1];
        new RestClient(false).create(UserRepository.class).getObject(HttpListener.of(UserResponse.class,
                response -> {
                    got[0] = response.getBody();
                    latch.countDown();
                }, error -> latch.countDown()));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Rina", got[0].getName());
        assertFalse(got[0].getId() == 0);
    }
}
