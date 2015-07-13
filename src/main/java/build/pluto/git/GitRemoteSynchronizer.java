package build.pluto.git;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.dependency.RemoteRequirement;
import build.pluto.git.dependency.GitRemoteRequirement;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;
import build.pluto.output.None;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.sugarj.common.FileCommands;

public class GitRemoteSynchronizer extends Builder<Input, None> {

    public static BuilderFactory<Input, None, GitRemoteSynchronizer> factory
        = BuilderFactory.of(GitRemoteSynchronizer.class, Input.class);

    public GitRemoteSynchronizer(Input input) {
        super(input);
    }

    @Override
    protected String description(Input input) {
        return "Keeps the directory " + input.directory.toString()
               + " in sync with " + input.url;
    }

    @Override
    protected File persistentPath(Input input) {
        if (input.summaryLocation != null) {
            return new File(input.summaryLocation, "git.dep");
        }
        return new File("./git.dep");
    }

    @Override
    protected None build(Input input) throws Throwable {
        if (!input.isValid()) {
            throw new IllegalArgumentException("Input was not correctly build.");
        }

        RemoteRequirement gitRequirement
            = new GitRemoteRequirement(input.url, input.directory, input.bound);
        this.require(gitRequirement);
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

        List<File> outputFiles = GitHandler.getNotIgnoredFilesOfRepo(input.directory);
        for(File f : outputFiles) {
            this.provide(f);
        }
        return None.val;
    }
}
