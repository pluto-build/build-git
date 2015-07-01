package build.pluto.git.bound;

import java.io.Serializable;

public interface UpdateBound extends Serializable {
    public String getBound();
    public String getBoundHash();
    public boolean reachedBound(String currentHash);
}
