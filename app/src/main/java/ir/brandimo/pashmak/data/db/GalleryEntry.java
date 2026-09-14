package ir.brandimo.pashmak.data.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A finished picture the child can revisit, share or export. */
@Entity(tableName = "gallery_entry")
public class GalleryEntry {

    public static final int KIND_MISSION_SCENE = 0;
    public static final int KIND_FREE_DRAW = 1;
    public static final int KIND_COLORING = 2;

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "kind")
    public int kind;

    /** -1 for pictures that do not belong to a mission. */
    @ColumnInfo(name = "missionIndex")
    public int missionIndex = -1;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @NonNull
    @ColumnInfo(name = "filePath")
    public String filePath = "";

    @ColumnInfo(name = "createdAt")
    public long createdAt;

    public GalleryEntry() {
    }
}
