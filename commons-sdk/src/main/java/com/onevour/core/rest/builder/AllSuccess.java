package com.onevour.core.rest.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * What the generated @OnAllSuccess code remembers: which calls of a group have succeeded, per screen
 * (weakly, a closed screen is forgotten). Main thread, like every listener.
 */
public final class AllSuccess {

    private static final Map<Object, Map<String, Set<String>>> SUCCEEDED = new WeakHashMap<>();

    private AllSuccess() {
    }

    /** A call of the group succeeded; true (and the group starts over) when all of them have. */
    public static synchronized boolean succeeded(Object screen, String group, String call, String... calls) {
        Map<String, Set<String>> groups = SUCCEEDED.get(screen);
        if (groups == null) {
            groups = new HashMap<>();
            SUCCEEDED.put(screen, groups);
        }
        Set<String> done = groups.get(group);
        if (done == null) {
            done = new HashSet<>();
            groups.put(group, done);
        }
        done.add(call);
        for (String name : calls) {
            if (!done.contains(name)) return false;
        }
        groups.remove(group);
        return true;
    }

    /** A call of the group failed: the group starts over. */
    public static synchronized void failed(Object screen, String group) {
        Map<String, Set<String>> groups = SUCCEEDED.get(screen);
        if (groups != null) groups.remove(group);
    }
}
