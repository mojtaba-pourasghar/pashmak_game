package ir.brandimo.pashmak.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {CapturedItem.class, GalleryEntry.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NAME = "pashmak.db";
    private static volatile AppDatabase instance;

    public abstract CapturedItemDao capturedItems();

    public abstract GalleryDao gallery();

    public static AppDatabase get(@NonNull Context context) {
        AppDatabase local = instance;
        if (local == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(), AppDatabase.class, NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
                local = instance;
            }
        }
        return local;
    }
}
