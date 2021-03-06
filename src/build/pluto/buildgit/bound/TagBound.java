package build.pluto.buildgit.bound;

import build.pluto.buildgit.util.GitHandler;

/**
 * Indicates that the repository only gets updated until its HEAD is tag.
 *
 */
public class TagBound implements UpdateBound {

    private static final long serialVersionUID = 3359487015008393192L;
    
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
}
