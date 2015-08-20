package build.pluto.git;

import build.pluto.builder.BuilderFactory;
import build.pluto.builder.RemoteAccessBuilder;
import build.pluto.git.dependency.GitRemoteRequirement;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;
import build.pluto.output.None;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.util.List;

public class GitRemoteSynchronizer extends RemoteAccessBuilder<GitInput, None> {

    public static BuilderFactory<GitInput, None, GitRemoteSynchronizer> factory
        = BuilderFactory.of(GitRemoteSynchronizer.class, GitInput.class);

    public GitRemoteSynchronizer(GitInput input) {
        super(input);
    }

    @Override
    protected String description(GitInput input) {
        return "Keeps the directory " + input.directory.toString()
               + " in sync with " + input.url;
    }

    @Override
    protected File persistentPath(GitInput input) {
        return new File(input.directory, ".git/git.dep");
    }

    @Override
    protected File timestampPersistentPath(GitInput input) {
        int urlHash = input.url.hashCode();
        String tsFileName = "git-" + urlHash + ".ts";
        File baseDir = input.summaryLocation != null ? input.summaryLocation : new File(".");
        return new File(input.summaryLocation, tsFileName);
    }

    @Override
    protected None build(GitInput input, File tsPersistentPath) throws Throwable {
        if (!input.isValid()) {
            throw new IllegalArgumentException("GitInput was not correctly build.");
        }

        GitRemoteRequirement gitRequirement
                = new GitRemoteRequirement(input.directory, input.bound, input.consistencyCheckInterval, tsPersistentPath);
        this.requireOther(gitRequirement);

        if (!FileCommands.exists(input.directory)
                || FileUtil.isDirectoryEmpty(input.directory)) {
            GitHandler.cloneRepository(input);
            if(input.bound != null) {
                GitHandler.resetRepoToCommit(input.directory,
                                             input.bound.getBoundHash());
            }
        } else {
            GitHandler.checkout(input.directory, input.bound.getBound());
            GitHandler.pull(input);
        }

        //provide files
        List<File> outputFiles = GitHandler.getNotIgnoredFilesOfRepo(input.directory);
        for(File f : outputFiles) {
            this.provide(f);
        }
        return None.val;
    }
}
