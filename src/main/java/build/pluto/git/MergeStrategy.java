package build.pluto.git;


public enum MergeStrategy {
    OURS {
        @Override
        public org.eclipse.jgit.merge.MergeStrategy getStrategy() {
            return org.eclipse.jgit.merge.MergeStrategy.OURS;
        }
    },
    RECURSIVE {
        @Override
        public org.eclipse.jgit.merge.MergeStrategy getStrategy() {
            return org.eclipse.jgit.merge.MergeStrategy.RECURSIVE;
        }
    },
    RESOLVE {
        @Override
        public org.eclipse.jgit.merge.MergeStrategy getStrategy() {
            return org.eclipse.jgit.merge.MergeStrategy.RESOLVE;
        }
    },
    SIMPLE_TWO_WAY_IN_CORE {
        @Override
        public org.eclipse.jgit.merge.MergeStrategy getStrategy() {
            return org.eclipse.jgit.merge.MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;
        }
    },
    THEIRS {
        @Override
        public org.eclipse.jgit.merge.MergeStrategy getStrategy() {
            return org.eclipse.jgit.merge.MergeStrategy.THEIRS;
        }
    };

    public abstract org.eclipse.jgit.merge.MergeStrategy getStrategy();
}
