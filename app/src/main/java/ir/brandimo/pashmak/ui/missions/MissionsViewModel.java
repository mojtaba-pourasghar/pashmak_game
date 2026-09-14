package ir.brandimo.pashmak.ui.missions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Map;

import ir.brandimo.pashmak.data.catalog.Mission;
import ir.brandimo.pashmak.data.catalog.MissionCatalog;
import ir.brandimo.pashmak.data.repo.MissionRepository;

public class MissionsViewModel extends AndroidViewModel {

    private final List<Mission> missions;
    private final LiveData<Map<Integer, Integer>> progress;

    public MissionsViewModel(@NonNull Application application) {
        super(application);
        missions = MissionCatalog.all(application);
        progress = MissionRepository.get(application).progress();
    }

    public List<Mission> missions() {
        return missions;
    }

    public LiveData<Map<Integer, Integer>> progress() {
        return progress;
    }

    /** The first unfinished mission, which the list highlights as "current". */
    public int currentMission(Map<Integer, Integer> done) {
        for (int i = 0; i < missions.size(); i++) {
            int captured = countFor(done, i);
            if (!missions.get(i).isComplete(captured)) {
                return i;
            }
        }
        return 0;
    }

    public static int countFor(Map<Integer, Integer> done, int missionIndex) {
        if (done == null) {
            return 0;
        }
        Integer value = done.get(missionIndex);
        return value == null ? 0 : value;
    }
}
