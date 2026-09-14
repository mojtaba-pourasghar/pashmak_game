package ir.brandimo.pashmak.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GalleryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GalleryEntry entry);

    @Query("SELECT * FROM gallery_entry ORDER BY createdAt DESC")
    LiveData<List<GalleryEntry>> observeAll();

    @Query("SELECT * FROM gallery_entry WHERE id = :id")
    GalleryEntry byId(long id);

    @Query("SELECT COUNT(*) FROM gallery_entry")
    LiveData<Integer> observeCount();

    @Query("DELETE FROM gallery_entry WHERE id = :id")
    void deleteById(long id);
}
