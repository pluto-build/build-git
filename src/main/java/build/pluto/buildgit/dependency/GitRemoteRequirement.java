package build.pluto.buildgit.dependency;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.dependency.RemoteRequirement;
import build.pluto.buildgit.bound.UpdateBound;
import build.pluto.buildgit.util.GitHandler;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class GitRemoteRequirement extends RemoteRequirement implements Serializable {

    private File directory;
    private UpdateBound bound;

    public GitRemoteRequirement(File directory, UpdateBound bound, long consistencyCheckInterval, File persistentPath) {
        super(persistentPath, consistencyCheckInterval);
        this.directory = directory;
        this.bound = bound;
    }


    public boolean isConsistentWithRemote() {
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

    @Override
    public boolean tryMakeConsistent(BuildUnitProvider manager) throws IOException {
        return this.isConsistent();
    }


    @Override
    public String toString() {
        return "GitRemoteReq(" + directory.toString() + ")";
    }
}
