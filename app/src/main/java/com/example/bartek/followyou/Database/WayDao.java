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

    @Query("SELECT * FROM way WHERE id = :wayId")
    Way getById(int wayId);

    @Insert
    void insert(Way way);

    @Delete
    void delete(Way way);

    @Update
    void update(Way way);

    @Query("DELETE FROM way WHERE id = :wayId")
    void deleteById(int wayId);

    @Query("SELECT * FROM way ORDER BY id DESC LIMIT 1")
    Way getLastWay();
}
