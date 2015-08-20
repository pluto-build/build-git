package build.pluto.git.bound;

import build.pluto.git.util.GitHandler;

/**
 * Indicates that the repository only gets updated until its HEAD is tag.
 *
 */
public class TagBound implements UpdateBound {

    private final String remote;
    private final String tag;

    public TagBound(String remote, String tag) {
        this.remote = remote;
        this.tag = tag;
    }

    public String getBound() {
        return this.tag;
    }

    public String getBoundHash() {
        String hashOfTag = GitHandler.getHashOfBound(this.remote, this);
        return hashOfTag;
    }

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(getBoundHash());
    }
}
