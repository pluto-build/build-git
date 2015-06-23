package build.pluto.gitbuilder;

import java.io.File;

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
            return new ValueStamp<String>(this, null);
        }
        String commitHashOfHEAD = GitHandler.getHashOfRemoteHEAD(this.url, this.branch);
        return new ValueStamp<String>(this, commitHashOfHEAD);
    }
}
