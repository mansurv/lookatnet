package com.netmontools.lookatnet.ui.local.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "FolderSize")
public class FolderSizeModel {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "path")
    public String path;

    @ColumnInfo(name = "size")
    public long size;

    @ColumnInfo(name = "mtime")
    public long mtime;

    public FolderSizeModel() {
    }

    public FolderSizeModel(String path, long size, long mtime) {
        this.path = path;
        this.size = size;
        this.mtime = mtime;
    }
}
