package com.example.bartek.followyou.Database;

import android.arch.persistence.room.TypeConverter;
import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * Created by bartek on 29.03.2018.
 */

public class Converters {
    @TypeConverter
    public static Location fromString(String value) {
        Type objectType = new TypeToken<Location>() {}.getType();
        return new Gson().fromJson(value, objectType);
    }

    @TypeConverter
    public static String fromObject(Location location) {
        Gson gson = new Gson();
        String json = gson.toJson(location);
        return json;
    }
}
