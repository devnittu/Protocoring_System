package com.examportal.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared Gson instance with LocalDateTime support. */
public final class JsonUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class,
            (JsonSerializer<LocalDateTime>) (src, t, ctx) ->
                ctx.serialize(src != null ? src.format(FMT) : null))
        .registerTypeAdapter(LocalDateTime.class,
            (JsonDeserializer<LocalDateTime>) (json, t, ctx) ->
                json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), FMT))
        .serializeNulls()
        .create();

    private JsonUtil() {}

    public static String toJson(Object o)           { return GSON.toJson(o); }
    public static <T> T fromJson(String s, Class<T> c) { return GSON.fromJson(s, c); }
}
