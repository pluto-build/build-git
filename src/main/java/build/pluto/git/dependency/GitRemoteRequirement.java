package build.pluto.git.dependency;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.dependency.RemoteRequirement;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.util.GitHandler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.sugarj.common.FileCommands;

public class GitRemoteRequirement implements RemoteRequirement {

    public static final long serialVersionUID = 12356L;

    private String url;
    private File directory;
    private UpdateBound bound;

    public GitRemoteRequirement(String url, File directory, UpdateBound bound) {
        this.url = url;
        this.directory = directory;
        this.bound = bound;
    }

    public boolean isConsistent() {
        if (!FileCommands.exists(directory)) {
            return false;
        }
        String currentHash = null;
        try {
            currentHash = GitHandler.getHashOfHEAD(directory);
        } catch (IOException e) {
            return true;
        }
        if (currentHash == null || !bound.reachedBound(currentHash)) {
            return false;
        }
        return true;
    }

    public boolean isConsistentInBuild(BuildUnitProvider manager)
            throws IOException {
        return this.isConsistent();
    }

    public boolean isTimeToLiveOver(long currentTime) {
        return true;
    }
}
