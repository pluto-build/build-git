package build.pluto.gitbuilder.stamp;

import java.io.File;

import build.pluto.gitbuilder.util.GitHandler;
import build.pluto.stamp.Stamper;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.ValueStamp;

import org.sugarj.common.FileCommands;

public class GitHashStamper implements Stamper {
    public static final long serialVersionUID = 1234L;

    public final String url;
    public final String branch;

    public GitHashStamper(String url, String branch) {
        this.url = url;
        this.branch = branch;
    }

    public Stamp stampOf(File p) {
        if (!FileCommands.exists(p)) {
            return new ValueStamp<>(this, null);
        }
        String commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD(this.url, this.branch);
        if (commitHashOfHEAD == null) {
            commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD("file://" + p.getAbsolutePath(), this.branch);
        }
        return new ValueStamp<>(this, commitHashOfHEAD);
    }
}
