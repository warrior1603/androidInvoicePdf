package com.chouchene.factures.utils;

import android.graphics.Color;

public class AvatarHelper {
    private static final String[] COLORS = {
            "#475569", "#4f46e5", "#059669", "#e11d48", "#d97706",
            "#0891b2", "#7c3aed", "#2563eb", "#db2777", "#16a34a",
            "#ca8a04", "#0284c7", "#9333ea", "#0d9488", "#ea580c"
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
