package com.chouchene.factures.utils;

import android.graphics.Color;

public class AvatarHelper {
    private static final String[] COLORS = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B"
    };

    public static int getColorForName(String name) {
        if (name == null || name.isEmpty()) return Color.parseColor(COLORS[0]);
        int index = Math.abs(name.hashCode()) % COLORS.length;
        return Color.parseColor(COLORS[index]);
    }

    public static String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].substring(0, 1).toUpperCase());
            }
        }
        return initials.length() > 0 ? initials.toString() : "?";
    }
}
