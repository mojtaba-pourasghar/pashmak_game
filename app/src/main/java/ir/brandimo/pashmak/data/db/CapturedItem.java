package ir.brandimo.pashmak.data.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One drawing the child scanned, cut out and added to a mission. */
@Entity(tableName = "captured_item",
        indices = {@Index(value = {"missionIndex", "slotIndex"}, unique = true)})
public class CapturedItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "missionIndex")
    public int missionIndex;

    @ColumnInfo(name = "slotIndex")
    public int slotIndex;

    @NonNull
    @ColumnInfo(name = "itemLabel")
    public String itemLabel = "";

    @NonNull
    @ColumnInfo(name = "pngPath")
    public String pngPath = "";

    @ColumnInfo(name = "createdAt")
    public long createdAt;

    public CapturedItem() {
    }
}
