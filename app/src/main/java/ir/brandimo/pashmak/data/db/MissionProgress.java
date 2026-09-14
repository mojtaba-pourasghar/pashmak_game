package ir.brandimo.pashmak.data.db;

import androidx.room.ColumnInfo;

/**
 * Progress is derived from the captured items rather than stored, so it can never
 * drift out of step with the files on disk.
 */
public class MissionProgress {

    @ColumnInfo(name = "missionIndex")
    public int missionIndex;

    @ColumnInfo(name = "done")
    public int done;

    public MissionProgress() {
    }
}
