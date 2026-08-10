package com.chouchene.factures.model;

public class AppNotification {
    public enum Type { INFO, ALERT, SUCCESS }
    public String title;
    public String description;
    public Type type;
    public boolean isRead;
    public int targetFragmentId;
    public int iconRes;

    public AppNotification(String title, String description, Type type, int targetFragmentId, int iconRes) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.targetFragmentId = targetFragmentId;
        this.iconRes = iconRes;
        this.isRead = false;
    }
}
