package build.pluto.gitbuilder;

import build.pluto.builder.Builder;
import build.pluto.output.None;
import org.eclipse.jgit.api.errors.*;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GitRepositoryBuilder extends Builder<Input, None> {

    public GitRepositoryBuilder(Input input) {
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
        if (!directoryExists(input) || directoryIsEmpty(input)) {
            if (git.isUrlAccessible()) {
                git.cloneRepository();
            } else {
                throw new TransportException(input.url + " can not be accessed");
            }
        } else {
            if (GitHandler.isRepo(input.directory) && git.isUrlSet()) {
                if (git.isUrlAccessible()) {
                    git.checkout(input.branchName);
                    git.pull();
                } else {
                    //do nothing
                }
            } else {
                throw new IllegalArgumentException(input.directory.toString() + " is not empty and does contains other data than the repository");
            }
        }

        //TODO: maybe only provide files not ignored by .gitignore
        List<Path> outputFiles = FileCommands.listFilesRecursive(input.directory.toPath());
        File gitDirectory = new File(input.directory, ".git");
        for (Path p : outputFiles) {
            if (!containsFile(gitDirectory, p.toFile())) {
                provide(p.toFile());
            }
        }
        return None.val;
    }

    public boolean directoryExists(Input input) {
        return FileCommands.exists(input.directory);
    }

    public boolean directoryIsEmpty(Input input) {
        return FileCommands.listFilesRecursive(input.directory.toPath()).size() == 0;
    }

    public boolean containsFile(File directory, File file) {
        return file.getAbsolutePath().contains(directory.getName() + "/");
    }
}
