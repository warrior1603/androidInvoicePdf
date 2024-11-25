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
        // Creating the app database with Room database builder
        // MyToDos is the name of the database
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
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

