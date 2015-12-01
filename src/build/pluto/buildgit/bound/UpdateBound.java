package build.pluto.buildgit.bound;

import java.io.Serializable;

/**
 * An UpdateBound is a mark which indicates until which commit, tag or branch
 * the repository gets updated to.
 */
public interface UpdateBound extends Serializable {
    public String getBound();
    public String getBoundHash();
}
