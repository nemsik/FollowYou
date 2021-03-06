package com.example.bartek.followyou.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Created by bartek on 28.03.2018.
 */

@Dao
public interface LocDao {
    @Insert
    void insert(Loc loc);

    @Update
    void update(Loc... locs);

    @Delete
    void delete(Loc... locs);
    @Query("SELECT * FROM loc")
    List<Loc> getAllRepos();

    @Query("SELECT * FROM loc WHERE wayId=:wayId")
    List<Loc> getLocsForWayID(int wayId);

    @Query("SELECT * FROM loc ORDER BY id DESC LIMIT 1")
    Loc getLastLoc();

    @Query("SELECT * FROM loc WHERE wayId=:wayId LIMIT 1")
    Loc getFirstLocById(int wayId);
}
