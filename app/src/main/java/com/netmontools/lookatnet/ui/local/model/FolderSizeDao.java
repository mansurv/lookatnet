package com.netmontools.lookatnet.ui.local.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FolderSizeDao {
    @Query("SELECT * FROM FolderSize WHERE path = :path LIMIT 1")
    FolderSizeModel byPath(String path);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FolderSizeModel model);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<FolderSizeModel> models);

    @Query("SELECT * FROM FolderSize WHERE path IN (:paths)")
    List<FolderSizeModel> byPaths(List<String> paths);

    @Query("SELECT COUNT(path) FROM FolderSize")
    int count();
}
