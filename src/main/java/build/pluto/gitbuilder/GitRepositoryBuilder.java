package build.pluto.gitbuilder;

import build.pluto.builder.Builder;
import build.pluto.output.None;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class GitRepositoryBuilder extends Builder<Input, None> {

    public GitRepositoryBuilder(Input input) {
        super(input);
    }

    @Override
    protected String description(Input input) {
        return "Keeps the directory " + input.local.toString() + " in sync with " + input.remote;
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
        //TODO: think I don't need to require any files
        GitHandler git = new GitHandler(input);
        if (!localExists(input) || localIsEmpty(input)) {
            if (git.isRemoteAccessible()) {
                git.cloneRepository();
            } else {
                throw new TransportException(input.remote + " can not be accessed");
            }
        } else {
            if (GitHandler.isRepo(input.local) && git.isRemoteSet()) {
                if (git.isRemoteAccessible()) {
                    git.pull();
                } else {
                    //do nothing
                }
            } else {
                throw new IllegalArgumentException(input.local.toString() + " is not empty and does contains other data than the repository");
            }
        }

        //TODO: maybe only provide files not ignored by .gitignore
        List<Path> outputFiles = FileCommands.listFilesRecursive(input.local.toPath());
        for (Path p : outputFiles) {
            if (!p.toAbsolutePath().toString().contains(".git")) {
                provide(p.toFile());
            }
        }
        return None.val;
    }

    public boolean localExists(Input input) {
        return FileCommands.exists(input.local);
    }

    public boolean localIsEmpty(Input input) {
        return FileCommands.listFilesRecursive(input.local.toPath()).size() == 0;
    }
}