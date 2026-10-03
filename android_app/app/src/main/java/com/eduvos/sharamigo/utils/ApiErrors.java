package com.eduvos.sharamigo.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Response;

public class ApiErrors {
    public static final String NO_CONNECTION = "Cannot reach the server. Check your connection.";

    // Reads the "message" field the PHP backend sends with errors.
    public static String message(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JsonObject obj = new JsonParser().parse(raw).getAsJsonObject();
                if (obj.has("message")) {
                    return obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
