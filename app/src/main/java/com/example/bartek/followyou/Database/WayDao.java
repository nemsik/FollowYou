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
public interface WayDao {
    @Query("SELECT * FROM way")
    List<Way> getAll();

    @Query("SELECT * FROM way WHERE id IN (:wayIds)")
    List<Way> loadAllByIds(int[] wayIds);

    @Query("SELECT * FROM way WHERE id IN (:wayId)")
    Way getById(int wayId);

    @Insert
    void insertAll(Way... ways);

    @Insert
    void insert(Way way);

    @Delete
    void delete(Way way);

    @Update
    void update(Way user);

    @Query("SELECT * FROM way WHERE id=(SELECT MAX(id) FROM way)")
    Way getWay();
}
