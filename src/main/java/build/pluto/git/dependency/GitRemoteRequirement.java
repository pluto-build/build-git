package build.pluto.git.dependency;

import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.git.dependency.RemoteRequirement;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.util.GitHandler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.sugarj.common.FileCommands;

public class GitRemoteRequirement extends RemoteRequirement implements Serializable {

    public static final long serialVersionUID = 12356L;

    private String url;
    private File directory;
    private UpdateBound bound;
    private long consistencyCheckInterval;

    public File persistentPath;

    public GitRemoteRequirement(String url, File directory, UpdateBound bound, long consistencyCheckInterval, File persistentPath) {
        this.url = url;
        this.directory = directory;
        this.bound = bound;
        this.consistencyCheckInterval = consistencyCheckInterval;
        this.persistentPath = persistentPath;
        if (!FileCommands.fileExists(persistentPath)) {
            Thread currentThread = Thread.currentThread();
            long currentTime = BuildManager.requireInitiallyTimeStamps.getOrDefault(currentThread, -1L);
            createPersistentPath();
        }
    }
    
    public void createPersistentPath() {
        try {
            FileCommands.createFile(persistentPath);
        } catch(IOException e) {}
    }
    public void writePersistentPath(long timeStamp) {
        System.out.println("NEW TS "+ timeStamp);
        try {
            FileCommands.writeToFile(persistentPath, String.valueOf(timeStamp));
        } catch(IOException e) {}
    }
    public long readPersistentPath() {
        try {
            String persistentPathContent = FileCommands.readFileAsString(persistentPath);
            return Long.parseLong(persistentPathContent.replace("\n", ""));
        } catch(IOException e) {
        } catch(NumberFormatException e) {
        }
        return -1L;
    }

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
        // System.out.println("CHECK");
        // long lastConsistencyCheck = readPersistentPath();
        // System.out.println("CURR "+ currentTime);
        // System.out.println("OLD "+ lastConsistencyCheck);
        // if (lastConsistencyCheck + consistencyCheckInterval > currentTime) {
        //     writePersistentPath(currentTime);
        //     return true;
        // }
        return true;
    }

    @Override
    public String toString() {
        return "GitRemoteReq(" + url + ", " + directory.toString() + ")";
    }
}
