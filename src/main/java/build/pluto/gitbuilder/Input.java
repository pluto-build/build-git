package build.pluto.gitbuilder;

import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.File;
import java.io.Serializable;

public class Input implements Serializable {
    private static final long serialVersionUID = -1L;
    public final File localLocation;
    public final String remoteLocation;
    public final String branchName;
    public final File summaryLocation;

    public final MergeCommand.FastForwardMode ffMode;
    public final MergeStrategy mergeStrategy;
    public final boolean createMergeCommit;
    public final boolean squashCommit;


    private Input(Builder builder) {
        this.localLocation = builder.localLocation;
        this.remoteLocation = builder.remoteLocation;
        this.branchName = builder.branchName;
        this.summaryLocation = builder.summaryLocation;
        this.ffMode = builder.ffMode;
        this.mergeStrategy = builder.mergeStrategy;
        this.createMergeCommit = builder.createMergeCommit;
        this.squashCommit = builder.squashCommit;
    }

    public static class Builder {
        private final File localLocation;
        private final String remoteLocation;
        private final String branchName;
        private final File summaryLocation;

        private MergeCommand.FastForwardMode ffMode = MergeCommand.FastForwardMode.FF_ONLY;
        private MergeStrategy mergeStrategy = MergeStrategy.RESOLVE;
        private boolean createMergeCommit = false;
        private boolean squashCommit = false;

        public Builder(File localLocation, String remoteLocation, String branchName, File summaryLocation) {
            this.localLocation = localLocation == null ? new File(".") : localLocation;
            this.remoteLocation = remoteLocation;
            this.summaryLocation = summaryLocation == null ? new File(".") : summaryLocation;
            this.branchName = branchName == null ? "master" : branchName;
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