package ir.brandimo.pashmak.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CapturedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CapturedItem item);

    @Query("SELECT missionIndex, COUNT(*) AS done FROM captured_item GROUP BY missionIndex")
    LiveData<List<MissionProgress>> observeProgress();

    @Query("SELECT missionIndex, COUNT(*) AS done FROM captured_item GROUP BY missionIndex")
    List<MissionProgress> progressNow();

    @Query("SELECT * FROM captured_item WHERE missionIndex = :missionIndex ORDER BY slotIndex ASC")
    LiveData<List<CapturedItem>> observeForMission(int missionIndex);

    @Query("SELECT * FROM captured_item WHERE missionIndex = :missionIndex ORDER BY slotIndex ASC")
    List<CapturedItem> forMission(int missionIndex);

    @Query("SELECT COUNT(*) FROM captured_item WHERE missionIndex = :missionIndex")
    int countForMission(int missionIndex);

    @Query("DELETE FROM captured_item WHERE missionIndex = :missionIndex AND slotIndex = :slotIndex")
    void deleteSlot(int missionIndex, int slotIndex);

    @Query("DELETE FROM captured_item WHERE missionIndex = :missionIndex")
    void clearMission(int missionIndex);
}
