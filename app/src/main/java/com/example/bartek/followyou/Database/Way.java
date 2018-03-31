package com.example.bartek.followyou.Database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by bartek on 28.03.2018.
 */

@Entity(tableName = "way")
public class Way {
    @PrimaryKey(autoGenerate = true)
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

}
