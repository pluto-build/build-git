package build.pluto.gitbuilder.bound;

import build.pluto.gitbuilder.util.GitHandler;
import java.io.File;

public class BranchBound implements UpdateBound {

    private final String branchName;
    private final String remote;
    
    public BranchBound(String branchName, String remote) {
        this.branchName = branchName;
        this.remote = remote;
    }

    public String getBoundHash() {
        String hashOfBranchHEAD = GitHandler.getHashOfRemoteHEAD(remote, branchName);
        return hashOfBranchHEAD;
    }

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(getBoundHash());
    }
}
