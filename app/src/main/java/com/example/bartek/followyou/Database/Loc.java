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

    @ColumnInfo(name = "time")
    private long time;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "speed")
    private double speed;


    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWayId() {
        return this.wayId;
    }

    public void setWayId(int wayId) {
        this.wayId = wayId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
