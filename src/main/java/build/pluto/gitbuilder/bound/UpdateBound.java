package build.pluto.gitbuilder.bound;

import build.pluto.gitbuilder.util.GitHandler;
import java.io.File;
import java.io.Serializable;

public interface UpdateBound extends Serializable {
    public String getBoundHash();
    public boolean reachedBound(String currentHash);
}
