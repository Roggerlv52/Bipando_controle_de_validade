package com.rogger.bp.data.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

public abstract class BpdDatabase extends RoomDatabase {

    private static volatile BpdDatabase INSTANCE;

    public static BpdDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (BpdDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BpdDatabase.class,
                                    "bp"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;}
    }