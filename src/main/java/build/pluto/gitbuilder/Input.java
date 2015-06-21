package build.pluto.gitbuilder;

import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.File;
import java.io.Serializable;

public class Input implements Serializable {
    private static final long serialVersionUID = -1L;
    public final File directory;
    public final String url;
    public final File summaryLocation;

    public final String branchName;
    public final MergeCommand.FastForwardMode ffMode;
    public final MergeStrategy mergeStrategy;
    public final boolean createMergeCommit;
    public final boolean squashCommit;


    private Input(Builder builder) {
        this.directory = builder.directory;
        this.url = builder.url;
        this.summaryLocation = builder.summaryLocation;
        this.branchName = builder.branchName;
        this.ffMode = builder.ffMode;
        this.mergeStrategy = builder.mergeStrategy;
        this.createMergeCommit = builder.createMergeCommit;
        this.squashCommit = builder.squashCommit;
    }

    public static class Builder {
        private final File directory;
        private final String url;
        private final File summaryLocation;

        private String branchName = "master";
        private MergeCommand.FastForwardMode ffMode = MergeCommand.FastForwardMode.FF_ONLY;
        private MergeStrategy mergeStrategy = MergeStrategy.RESOLVE;
        private boolean createMergeCommit = false;
        private boolean squashCommit = false;

        public Builder(File directory, String url, File summaryLocation) {
            this.directory = directory == null ? new File(".") : directory;
            this.url = url;
            this.summaryLocation = summaryLocation == null ? new File(".") : summaryLocation;
        }

        public Builder setBranchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public Builder setFfMode(MergeCommand.FastForwardMode ffMode) {
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

        public Input build() {
            return new Input(this);
        }
    }
}