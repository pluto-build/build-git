package build.pluto.gitbuilder.stamp;

import build.pluto.gitbuilder.util.GitHandler;
import build.pluto.gitbuilder.bound.UpdateBound;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.Stamper;
import build.pluto.stamp.ValueStamp;
import org.sugarj.common.FileCommands;

import java.io.File;

public class RemoteHashStamper implements Stamper {
    public static final long serialVersionUID = 1234L;

    public final String url;
    public final String branch;
    public final UpdateBound bound;

    public RemoteHashStamper(String url, String branch, UpdateBound bound) {
        this.url = url;
        this.branch = branch;
        this.bound = bound;
    }

    public Stamp stampOf(File p) {
        if (!FileCommands.exists(p)) {
            return new ValueStamp<>(this, null);
        }
        String commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD(this.url, this.branch);
        if (commitHashOfHEAD == null) {
            commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD("file://" + p.getAbsolutePath(), this.branch);
        }
        if(boundDefined() && bound.reachedBound(commitHashOfHEAD)) {
            return new ValueStamp<>(this, this.bound.getBoundHash());
        }
        return new ValueStamp<>(this, commitHashOfHEAD);
    }

    private boolean boundDefined() {
        return this.bound != null;
    }
}
