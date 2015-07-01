package build.pluto.git;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.git.stamp.RemoteHashStamper;
import build.pluto.git.util.FileUtil;
import build.pluto.git.util.GitHandler;
import build.pluto.output.None;
import org.eclipse.jgit.api.errors.TransportException;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GitRemoteSynchronizer extends Builder<Input, None> {

    public static BuilderFactory<Input, None, GitRemoteSynchronizer> factory = BuilderFactory.of(GitRemoteSynchronizer.class, Input.class);
    
    public GitRemoteSynchronizer(Input input) {
        super(input);
    }

    @Override
    protected String description(Input input) {
        return "Keeps the directory " + input.directory.toString() + " in sync with " + input.url;
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
        GitHandler git = new GitHandler(input);
        this.require(input.directory, new RemoteHashStamper(input.url, input.bound));
        if (!FileCommands.exists(input.directory) || FileUtil.directoryIsEmpty(input.directory)) {
            if (git.isUrlAccessible()) {
                git.cloneRepository();
                if(input.bound != null) {
                    GitHandler.resetRepoToCommit(input.directory, input.bound.getBoundHash());
                }
            } else {
                throw new TransportException(input.url + " can not be accessed");
            }
        } else {
            if (GitHandler.isRepo(input.directory) && git.isUrlSet()) {
                git.checkout(input.bound.getBound());
                git.pull();
            } else {
                throw new IllegalArgumentException(input.directory.toString() + " is not empty and does contains other data than the repository");
            }
        }

        //TODO: maybe only provide files not ignored by .gitignore
        List<Path> outputFiles = FileCommands.listFilesRecursive(input.directory.toPath());
        File gitDirectory = new File(input.directory, ".git");
        for (Path p : outputFiles) {
            if (!FileUtil.containsFile(gitDirectory, p.toFile())) {
                provide(p.toFile());
            }
        }
        return None.val;
    }
}
