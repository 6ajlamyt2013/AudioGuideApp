package com.example.audioguideai.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Удаляем старую таблицу и создаем новую
        database.execSQL("DROP TABLE IF EXISTS poi")
        database.execSQL("""
            CREATE TABLE poi (
                osmId INTEGER NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                category TEXT NOT NULL,
                categories TEXT NOT NULL,
                distanceFromUser REAL NOT NULL,
                isAnnounced INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Обновляем таблицу history
        database.execSQL("DROP TABLE IF EXISTS history")
        database.execSQL("""
            CREATE TABLE history (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                poiOsmId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                distance REAL NOT NULL,
                categories TEXT NOT NULL,
                categoryIcon TEXT NOT NULL
            )
        """.trimIndent())
    }
}

