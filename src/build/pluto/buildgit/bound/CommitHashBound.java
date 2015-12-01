package build.pluto.buildgit.bound;

/**
 * Indicates that the repository only gets updated until its HEAD is the
 * commit with the hash commitHash.
 *
 */
public class CommitHashBound implements UpdateBound {

    private static final long serialVersionUID = 2820137937298595751L;
    
	private final String commitHash;

    public CommitHashBound(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getBound() {
        return this.commitHash;
    }

    public String getBoundHash() {
        return this.commitHash;
    }
}
