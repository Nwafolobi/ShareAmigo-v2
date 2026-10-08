package com.eduvos.sharamigo.utils;

import java.util.Map;

// Shared listing rules so the form, profile and server agree.
public final class Listings {

    public static final int MAX_PHOTOS = 4;

    public static final String[] CATEGORIES = {"Textbooks", "Stationery", "Food/Meals", "Clothing"};
    public static final String[] CONDITIONS = {"Brand New", "Like New", "Good", "Fair"};

    private Listings() {}

    // Matches categories.max_credit_limit in the database.
    public static int maxCredits(String category) {
        if ("Textbooks".equals(category)) return 30;
        if ("Food/Meals".equals(category)) return 15;
        return 20; // Stationery, Clothing
    }

    // Friendly label for items.status
    public static String statusLabel(String status) {
        if ("Available".equals(status)) return "On the feed";
        if ("Escrow".equals(status)) return "Claimed · awaiting handover";
        if ("Exchanged".equals(status)) return "Exchanged";
        return status == null ? "" : status;
    }

    // Reads a whole number from a Gson map value (Gson turns JSON numbers into Double).
    public static int intValue(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
