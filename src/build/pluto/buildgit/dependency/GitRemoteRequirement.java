package build.pluto.buildgit.dependency;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.buildgit.GitException;
import build.pluto.buildgit.bound.UpdateBound;
import build.pluto.buildgit.util.FileUtil;
import build.pluto.buildgit.util.GitHandler;
import build.pluto.dependency.RemoteRequirement;

public class GitRemoteRequirement extends RemoteRequirement implements Serializable {

    private static final long serialVersionUID = 33598143840957248L;
    
	private File directory;
    private UpdateBound bound;
    private String url;

    public GitRemoteRequirement(File directory,
            UpdateBound bound,
            String url,
            long consistencyCheckInterval,
            File persistentPath) {
        super(persistentPath, consistencyCheckInterval);
        this.directory = directory;
        this.bound = bound;
        this.url = url;
    }


    public boolean isConsistentWithRemote() {
        if (!FileCommands.exists(directory)) {
            return false;
        }
        String currentHash = null;
        try {
            currentHash = GitHandler.getHashOfHEAD(directory);
        } catch (IOException | GitException e) {
            return true;
        }
        if (currentHash == null || !bound.getBoundHash().equals(currentHash)) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean isRemoteResourceAccessible() {
        return GitHandler.isUrlAccessible(url);
    }

    @Override
    protected boolean isLocalResourceAvailable() {
        if (!FileUtil.isDirectoryEmpty(directory)) {
            try {
				return GitHandler.isUrlSet(directory, url);
			} catch (GitException e) {
			}
        }
        return false;
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
