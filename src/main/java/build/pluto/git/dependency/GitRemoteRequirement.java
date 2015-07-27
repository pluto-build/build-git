package build.pluto.git.dependency;

import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.stamp.Stamper;
import build.pluto.stamp.Stamp;
import build.pluto.git.dependency.RemoteRequirement;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.util.GitHandler;
import build.pluto.dependency.FileRequirement;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.sugarj.common.FileCommands;

public class GitRemoteRequirement extends RemoteRequirement implements Serializable {

    public static final long serialVersionUID = 12356L;

    private File directory;
    private UpdateBound bound;
    private long consistencyCheckInterval;

    public File persistentPath;

    public GitRemoteRequirement(File directory, UpdateBound bound, long consistencyCheckInterval, File persistentPath) {
        this.directory = directory;
        this.bound = bound;
        this.consistencyCheckInterval = consistencyCheckInterval;
        this.persistentPath = persistentPath;
    }

    private void writePersistentPath(long timeStamp) {
        try {
            FileCommands.writeToFile(persistentPath, String.valueOf(timeStamp));
        } catch(IOException e) {}
    }

    private long readPersistentPath() {
        try {
            String persistentPathContent = FileCommands.readFileAsString(persistentPath);
            return Long.parseLong(persistentPathContent.replace("\n", ""));
        } catch(IOException e) {
        } catch(NumberFormatException e) {
        }
        return -1L;
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
    public boolean needsConsistencyCheck(long currentTime) {
        if (!FileCommands.exists(persistentPath)) {
           return true;
        }
        long lastConsistencyCheck = readPersistentPath();
        if (lastConsistencyCheck + consistencyCheckInterval < currentTime) {
            writePersistentPath(currentTime);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "GitRemoteReq(" + directory.toString() + ")";
    }
}
