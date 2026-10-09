package com.onevour.core.utilities.commons;

import android.app.Application;

import java.util.concurrent.atomic.AtomicReference;

public class ContextHelper {

    private final static AtomicReference<Application> context = new AtomicReference<>();

    public static void init(Application application) {
        context.set(application);
        RefSessionStore.preload(application);          // RefSession values load in the background
    }

    public static Application getApplication() {
        return context.get();
    }
}
