package build.pluto.git.bound;

import build.pluto.git.util.GitHandler;

public class BranchBound implements UpdateBound {

    private final String branchName;
    private final String remote;
    
    public BranchBound(String remote, String branchName) {
        this.branchName = branchName;
        this.remote = remote;
    }

    public String getBound() {
        return this.branchName;
    }

    public String getBoundHash() {
        String hashOfBranchHEAD = GitHandler.getHashOfRemoteHEAD(remote, branchName);
        return hashOfBranchHEAD;
    }

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(getBoundHash());
    }
}
