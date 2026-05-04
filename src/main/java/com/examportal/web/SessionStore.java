package com.examportal.web;

import com.examportal.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory token → User store for web sessions. */
public final class SessionStore {

    private static final Map<String, User> store = new ConcurrentHashMap<>();

    private SessionStore() {}

    public static String create(User user) {
        String token = UUID.randomUUID().toString();
        store.put(token, user);
        return token;
    }

    public static User get(String token) {
        return token == null ? null : store.get(token);
    }

    public static void remove(String token) {
        if (token != null) store.remove(token);
    }

    public static boolean isAdmin(String token) {
        User u = get(token);
        return u != null && u.isAdmin();
    }
}
