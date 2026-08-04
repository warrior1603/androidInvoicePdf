package com.chouchene.factures.database;

import android.content.Context;
import androidx.room.Room;

public class DatabaseClient {

    private Context context;
    private static DatabaseClient instance;

    // Our app database object
    private AppDatabase appDatabase;

    private DatabaseClient(Context context) {
        this.context = context;
        // Using a new name "MyClientsV8" to force a fresh schema match and avoid integrity crashes
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "MyClientsV8")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}

