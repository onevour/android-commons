package com.onevour.sdk.impl.modules.rest;

import com.google.gson.annotations.Expose;

/** JSONPlaceholder (jsonplaceholder.typicode.com) models of the REST sample: a fake API that answers every verb. */
public final class Placeholder {

    private Placeholder() {
    }

    public static class Post {
        @Expose public Integer id;
        @Expose public Integer userId;
        @Expose public String title;
        @Expose public String body;

        public Post() {
        }

        public Post(Integer id, Integer userId, String title, String body) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.body = body;
        }
    }

    /** Only the fields a PATCH changes. */
    public static class PostTitle {
        @Expose public String title;

        public PostTitle(String title) {
            this.title = title;
        }
    }

    public static class Comment {
        @Expose public int id;
        @Expose public int postId;
        @Expose public String name;
        @Expose public String email;
    }
}
