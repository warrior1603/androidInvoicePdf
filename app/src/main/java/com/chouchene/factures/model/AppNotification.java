package com.chouchene.factures.model;

public class AppNotification {
    public enum Type { INFO, ALERT, SUCCESS }
    public String title;
    public String description;
    public Type type;
    public boolean isRead;

    public AppNotification(String title, String description, Type type) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.isRead = false;
    }
}
