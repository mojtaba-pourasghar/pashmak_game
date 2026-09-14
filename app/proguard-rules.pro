# Room generated implementations are referenced reflectively by the runtime.
-keep class ir.brandimo.pashmak.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
