package com.rashot.audioguideai.data.db

@Database(entities = [PointOfInterest::class], version = 1)
abstract class POIDatabase : RoomDatabase() {
    abstract fun poiDao(): POIDao

    companion object {
        @Volatile
        private var INSTANCE: POIDatabase? = null

        fun getDatabase(context: Context): POIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    POIDatabase::class.java,
                    "poi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}