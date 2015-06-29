package build.pluto.gitbuilder.stamp;

import java.io.File;

import build.pluto.gitbuilder.util.GitHandler;
import build.pluto.stamp.Stamper;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.ValueStamp;

import org.sugarj.common.FileCommands;

public class RemoteHashStamper implements Stamper {
    public static final long serialVersionUID = 1234L;

    public final String url;
    public final String branch;
    public final String commitBound;

    public RemoteHashStamper(String url, String branch, String commitBound) {
        this.url = url;
        this.branch = branch;
        this.commitBound = commitBound;
    }

    public Stamp stampOf(File p) {
        if (!FileCommands.exists(p)) {
            return new ValueStamp<>(this, null);
        }
        String commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD(this.url, this.branch);
        if (commitHashOfHEAD == null) {
            commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD("file://" + p.getAbsolutePath(), this.branch);
        }
        if(commitBoundDefined() && !commitHashOfHEAD.equals(this.commitBound)) {
            return new ValueStamp<>(this, this.commitBound);
        }
        return new ValueStamp<>(this, commitHashOfHEAD);
    }

    private boolean commitBoundDefined() {
        return this.commitBound != null;
    }
}
