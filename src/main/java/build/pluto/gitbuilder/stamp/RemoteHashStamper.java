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
    public final UpdateBound bound;

    public RemoteHashStamper(String url, UpdateBound bound) {
        this.url = url;
        this.bound = bound;
    }

    public Stamp stampOf(File p) {
        if (!FileCommands.exists(p)) {
            return new ValueStamp<>(this, null);
        }
        String currentHash = GitHandler.getHashOfBound(this.url, this.bound);
        if(currentHash != null && bound.reachedBound(currentHash)) {
            return new ValueStamp<>(this, this.bound.getBoundHash());
        }
        return new ValueStamp<>(this, bound.getBoundHash());
    }
}
