package build.pluto.git;

import build.pluto.builder.Builder;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuilderFactory;
import build.pluto.git.dependency.RemoteRequirement;
import build.pluto.git.dependency.GitRemoteRequirement;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;
import build.pluto.output.None;

import java.io.File;
import java.util.List;

import org.sugarj.common.FileCommands;

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
        if (input.summaryLocation != null) {
            return new File(input.summaryLocation, "git.dep");
        }
        return new File("./git.dep");
    }

    @Override
    protected None build(GitInput input) throws Throwable {
        if (!input.isValid()) {
            throw new IllegalArgumentException("GitInput was not correctly build.");
        }

        File timeStampPersistentPath = new File(input.summaryLocation, "ts.dep");
        GitRemoteRequirement gitRequirement
                = new GitRemoteRequirement(input.url, input.directory, input.bound, input.consistencyCheckInterval, timeStampPersistentPath);
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

        //Write timestamp to file
        Thread currentThread = Thread.currentThread();
        long currentTime = BuildManager.requireInitiallyTimeStamps.get(currentThread);
        FileCommands.createFile(timeStampPersistentPath);
        FileCommands.writeToFile(timeStampPersistentPath, String.valueOf(currentTime));

        //provide files
        List<File> outputFiles = GitHandler.getNotIgnoredFilesOfRepo(input.directory);
        for(File f : outputFiles) {
            this.provide(f);
        }
        return None.val;
    }
}
