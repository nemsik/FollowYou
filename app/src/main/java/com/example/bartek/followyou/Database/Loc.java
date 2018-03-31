package com.example.bartek.followyou.Database;

/**
 * Created by bartek on 28.03.2018.
 */

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Way.class,
        parentColumns = "id",
        childColumns = "wayId",
        onDelete = CASCADE))

public class Loc {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo
    private int wayId;

    @ColumnInfo(name = "location")
    private Location location;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWayId() {
        return wayId;
    }

    public void setWayId(int wayId) {
        this.wayId = wayId;
    }
}
