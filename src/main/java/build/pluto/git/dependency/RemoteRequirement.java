package build.pluto.git.dependency;

import build.pluto.dependency.Requirement;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildManager;

import java.io.IOException;

public abstract class RemoteRequirement implements Requirement {

    /**
     * This implementation calls isTimeToLiveOver and isConsistentWithRemote
     */ 
    public boolean isConsistent() {
        Thread currentThread = Thread.currentThread();
        long timeStamp = BuildManager.requireInitiallyTimeStamps.getOrDefault(currentThread, 0L);
        if(needsConsistencyCheck(timeStamp)) {
            System.out.println("CHECK REMOTE");
            return isConsistentWithRemote();
        }
        return true;
    }

    /**
     * Checks if the local state is consistent with the remote state.
     * @return true if local state is consistent with remote state.
     */
    public abstract boolean isConsistentWithRemote();

    /**
     * Checks if a consistency check needs to be made.
     * @param currentTime the time to check if the consistency needs to be checked.
     * @return true if a consistency check needs to be made.
     */
    public abstract boolean needsConsistencyCheck(long currentTime);
}
