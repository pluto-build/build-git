package build.pluto.git;

import build.pluto.git.bound.UpdateBound;
import build.pluto.git.bound.BranchBound;
import build.pluto.git.util.GitHandler;
import build.pluto.git.util.FileUtil;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Input implements Serializable {
    private static final long serialVersionUID = -1L;
    public final File directory;
    public final String url;
    public final File summaryLocation;

    public final List<String> branchesToClone;
    public final boolean cloneSubmodules;
    public final FastForwardMode ffMode;
    public final MergeStrategy mergeStrategy;
    public final boolean createMergeCommit;
    public final boolean squashCommit;
    public final UpdateBound bound;


    private Input(Builder builder) {
        this.directory = builder.directory;
        this.url = builder.url;
        this.summaryLocation = builder.summaryLocation;
        this.branchesToClone = builder.branchesToClone;
        this.cloneSubmodules = builder.cloneSubmodules;
        this.ffMode = builder.ffMode;
        this.mergeStrategy = builder.mergeStrategy;
        this.createMergeCommit = builder.createMergeCommit;
        this.squashCommit = builder.squashCommit;
        this.bound = builder.bound;
    }

    public boolean isValid() {
        if (!GitHandler.isUrlAccessible(url)) {
            return false;
        }
        if (!FileUtil.isDirectoryEmpty(directory)) {
            if (GitHandler.isRepo(directory)) {
                return GitHandler.isUrlSet(directory, url);
            }
            return false;
        }
        return true;
    }

    public static class Builder {
        private final File directory;
        private final String url;
        private final File summaryLocation;

        private List<String> branchesToClone = new ArrayList<>();
        private boolean cloneSubmodules = false;
        private FastForwardMode ffMode = FastForwardMode.FF_ONLY;
        private MergeStrategy mergeStrategy = MergeStrategy.RESOLVE;
        private boolean createMergeCommit = false;
        private boolean squashCommit = false;
        private UpdateBound bound = null;

        public Builder(File directory, String url, File summaryLocation) {
            this.directory = directory == null ? new File(".") : directory;
            this.url = url;
            this.summaryLocation = summaryLocation == null ? new File(".") : summaryLocation;
            this.bound = new BranchBound(url, "master");
        }

        public Builder addBranchToClone(String branchName) {
            this.branchesToClone.add(branchName);
            return this;
        }

        public Builder setCloneSubmodules(boolean cloneSubmodules) {
            this.cloneSubmodules = cloneSubmodules;
            return this;
        }

        public Builder setFfMode(FastForwardMode ffMode) {
            this.ffMode = ffMode;
            return this;
        }

        public Builder setMergeStrategy(MergeStrategy mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        public Builder setCreateMergeCommit(boolean createMergeCommit) {
            this.createMergeCommit = createMergeCommit;
            return this;
        }

        public Builder setSquashCommit(boolean squashCommit) {
            this.squashCommit = squashCommit;
            return this;
        }

        public Builder setBound(UpdateBound bound) {
            this.bound = bound;
            return this;
        }

        public Input build() {
            return new Input(this);
        }
    }
}
