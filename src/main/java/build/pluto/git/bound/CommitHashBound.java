package build.pluto.git.bound;

/**
 * Indicates that the repository only gets updated until its HEAD is the
 * commit with the hash commitHash.
 *
 */
public class CommitHashBound implements UpdateBound {

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

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(commitHash);
    }
}
