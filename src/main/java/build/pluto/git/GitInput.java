package build.pluto.git;

import build.pluto.git.bound.BranchBound;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GitInput implements Serializable {
    private static final long serialVersionUID = 23456L;
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
    public transient long consistencyCheckInterval;


    private GitInput(Builder builder) {
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
        this.consistencyCheckInterval = builder.consistencyCheckInterval;
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
        private transient long consistencyCheckInterval = -1L;

        /**
         * @param directory in which the repository gets cloned into.
         * @param url which remote repository gets cloned.
         * @param summaryLocation where the build summary is saved.
         */
        public Builder(File directory, String url, File summaryLocation) {
            this.directory = directory == null ? new File(".") : directory;
            this.url = url;
            this.summaryLocation = summaryLocation == null ? new File(".") : summaryLocation;
            this.bound = new BranchBound(url, "master");
        }

        /**
         * Adds a branch thats get cloned when the builder is first executed.
         *
         * @param branchName the name of the branch you want to get cloned.
         */
        public Builder addBranchToClone(String branchName) {
            this.branchesToClone.add(branchName);
            return this;
        }

        /**
         * @param cloneSubmodules true if you want to get the submodules get
         * cloned. The default is false.
         */
        public Builder setCloneSubmodules(boolean cloneSubmodules) {
            this.cloneSubmodules = cloneSubmodules;
            return this;
        }

        /**
         * @param ffMode indicates the mode you want the fast forward to happen.
         * You can choose between  FF, FF_ONLY, and NO_FF. The default is FF_ONLY.
         */
        public Builder setFfMode(FastForwardMode ffMode) {
            this.ffMode = ffMode;
            return this;
        }

        /**
         * @param mergeStrategy is the strategy in which merges get performed.
         * You can choose between OURS, RECURSIVE,RESOLVE, SIMPLE_TWO_WAY_IN_CORE
         * and THREIS. The default is RESOLVE.
         */
        public Builder setMergeStrategy(MergeStrategy mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        /**
         * @param createMergeCommit is true if you want a commit to be created
         * if a merge gets performed. The default is false.
         */
        public Builder setCreateMergeCommit(boolean createMergeCommit) {
            this.createMergeCommit = createMergeCommit;
            return this;
        }
        /**
         *
         * @param squashCommit is true if a merge gets performed a squash also
         * gets performed. The default is false.
         */
        public Builder setSquashCommit(boolean squashCommit) {
            this.squashCommit = squashCommit;
            return this;
        }

        /**
         * @param bound is the bound of repository that it has to follow.
         * The default is the master branch.
         */
        public Builder setBound(UpdateBound bound) {
            this.bound = bound;
            return this;
        }

        /**
         * @param consistencyCheckInterval how long the builder waits til the
         * consistency of the builder gets checked again in milliseconds.
         * The default is 0;
         */
        public Builder setConsistencyCheckInterval(long consistencyCheckInterval) {
            this.consistencyCheckInterval = consistencyCheckInterval;
            return this;
        }

        public GitInput build() {
            return new GitInput(this);
        }
    }
}
