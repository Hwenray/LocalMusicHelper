package com.hwinzniej.musichelper.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hwinzniej.musichelper.data.dao.MusicDao
import com.hwinzniej.musichelper.data.model.Music

@Database(entities = [Music::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}