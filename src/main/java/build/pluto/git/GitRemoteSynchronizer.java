package build.pluto.git;

import build.pluto.builder.BuilderFactory;
import build.pluto.builder.Builder;
import build.pluto.git.dependency.GitRemoteRequirement;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;
import build.pluto.output.None;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.util.List;

public class GitRemoteSynchronizer extends Builder<GitInput, None> {

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
    protected None build(GitInput input) throws Throwable {
        if (!isInputValid(input)) {
            throw new IllegalArgumentException("GitInput was not correctly build.");
        }
        File tsPersistentPath = new File(input.directory, ".git/git.dep.time");

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

        //need to create requirement after the repo gets cloned because
        //the directory contains git.time.dep inside of .git when the
        //constructor gets called
        GitRemoteRequirement gitRequirement = new GitRemoteRequirement(
                input.directory,
                input.bound,
                input.consistencyCheckInterval,
                tsPersistentPath);
        this.requireOther(gitRequirement);

        //provide files
        List<File> outputFiles = GitHandler.getNotIgnoredFilesOfRepo(input.directory);
        for(File f : outputFiles) {
            this.provide(f);
        }
        return None.val;
    }

    private boolean isInputValid(GitInput input) {
        if (!GitHandler.isUrlAccessible(input.url)) {
            return false;
        }
        if (!FileUtil.isDirectoryEmpty(input.directory)) {
            if (GitHandler.isRepo(input.directory)) {
                return GitHandler.isUrlSet(input.directory, input.url);
            }
            return false;
        }
        return true;
    }
}
