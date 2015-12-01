package build.pluto.buildgit.bound;

import build.pluto.buildgit.util.GitHandler;

/**
 * Indicates that the repository only gets updated until its HEAD has the commit
 * hash of the head of the branch.
 *
 */
public class BranchBound implements UpdateBound {

    private static final long serialVersionUID = -1178775937862626673L;
    
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
}
