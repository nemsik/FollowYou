package com.example.bartek.followyou.Database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

/**
 * Created by bartek on 28.03.2018.
 */

@Database(entities = {Loc.class, Way.class}, version = 2)
@TypeConverters({Converters.class})

public abstract class AppDatabase extends RoomDatabase {
    public abstract LocDao locDao();
    public abstract WayDao wayDao();
}