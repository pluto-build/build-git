package build.pluto.buildgit;

import org.eclipse.jgit.api.MergeCommand;

public enum FastForwardMode {
    FF {
        @Override
        public MergeCommand.FastForwardMode getMode() {
            return MergeCommand.FastForwardMode.FF;
        }
    }, FF_ONLY {
        @Override
        public MergeCommand.FastForwardMode getMode() {
            return MergeCommand.FastForwardMode.FF_ONLY;
        }
    }, NO_FF {
        @Override
        public MergeCommand.FastForwardMode getMode() {
            return MergeCommand.FastForwardMode.NO_FF;
        }
    };

    public abstract MergeCommand.FastForwardMode getMode();
}
