package build.pluto.buildgit;

import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.Builder;
import build.pluto.buildgit.dependency.GitRemoteRequirement;
import build.pluto.buildgit.util.FileUtil;
import build.pluto.buildgit.util.GitHandler;
import build.pluto.output.None;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.util.List;

public class GitRemoteSynchronizer extends Builder<GitInput, None> {

    public static BuilderFactory<GitInput, None, GitRemoteSynchronizer> factory
        = BuilderFactoryFactory.of(GitRemoteSynchronizer.class, GitInput.class);

    public GitRemoteSynchronizer(GitInput input) {
        super(input);
    }

    @Override
    protected String description(GitInput input) {
        return "Git sync " + input.directory + " with remote " + input.url + " at " + input.bound.getBound();
    }

    @Override
    public File persistentPath(GitInput input) {
        return new File(input.directory, ".git/git.dep");
    }

    @Override
    protected None build(GitInput input) throws Throwable {
        isInputValid(input);
        if (!FileCommands.exists(input.directory)
                || FileUtil.isDirectoryEmpty(input.directory)) {
            GitHandler.cloneRepository(input);
            if(input.bound != null) {
                GitHandler.resetRepoToCommit(input.directory,
                                             input.bound.getBoundHash());
            }
        } else {
            GitHandler.checkout(input.directory, input.bound.getBound());
            // do not pull if no connection can be made so the builder does
            // not fail
            if(GitHandler.isUrlAccessible(input.url)) {
                GitHandler.pull(input);
            }
        }

        //need to create requirement after the repo gets cloned because
        //the directory contains git.time.dep inside of .git when the
        //constructor gets called
        File tsPersistentPath = new File(input.directory, ".git/git.dep.time");
        GitRemoteRequirement gitRequirement = new GitRemoteRequirement(
                input.directory,
                input.bound,
                input.url,
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

    private void isInputValid(GitInput input) {
        if (!FileUtil.isDirectoryEmpty(input.directory)) {
            if (GitHandler.isRepo(input.directory)) {
                boolean isUrlSet = GitHandler.isUrlSet(input.directory, input.url);
                if(!isUrlSet) {
                    throw new IllegalArgumentException(input.directory + " has " + input.url + " not set as remote");
                } else {
                    return;
                }
            } else {
                throw new IllegalArgumentException(input.directory + " contains other data");
            }
        }
        if (!GitHandler.isUrlAccessible(input.url)) {
            throw new IllegalArgumentException(input.url + " can not be accessed");
        }
    }
}
