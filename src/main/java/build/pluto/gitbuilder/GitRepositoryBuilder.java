package build.pluto.gitbuilder;

import build.pluto.builder.Builder;
import build.pluto.output.None;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
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
        if (!localExists(input) || localIsEmpty(input)) {
            if (isRemoteAccessible(input)) {
                cloneRepository(input);
            } else {
                throw new TransportException(input.remote + " can not be accessed");
            }
        } else {
            if (isLocalRepo(input)) {
                if (isRemoteAccessible(input) && isRemoteSet(input)) {
                    pullRepository(input);
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

    public boolean isRemoteAccessible(Input input) {
        try {
            Git.lsRemoteRepository().setRemote(input.remote).call();
        } catch (InvalidRemoteException e) {
            return false;
        } catch (TransportException e) {
            return false;
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void cloneRepository(Input input) {
        try {
            Git result = Git.cloneRepository()
                    .setURI(input.remote)
                    .setDirectory(input.local)
                    .setBranch(input.branchName)
                    .call();
        } catch (InvalidRemoteException e) {
            System.out.println("INVALID REPO");
        } catch (TransportException e) {
            System.out.println("TRANSPORT FAILURE");
//            throw e;
        } catch (GitAPIException e) {
            System.out.println("GIT FAILURE");
        }
    }

    public boolean isLocalRepo(Input input) {
        try {
            Git.open(input.local);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean isRemoteSet(Input input) {
        try {
            StoredConfig config = Git.open(input.local).getRepository().getConfig();
            Set<String> remotes = config.getSubsections("remote");
            boolean foundRemote = false;
            for (String remote : remotes) {
                String url = config.getString("remote", remote, "url");
                if (url.equals(input.remote)) {
                    foundRemote = true;
                }
            }
            return foundRemote;
        } catch (IOException e) {
            return false;
        }
    }

    public void pullRepository(Input input) {
        try {
            Git g = Git.open(input.local);

            FetchCommand fetch = g.fetch();
            FetchResult fetchResult = fetch.call();

            MergeCommand merge = g.merge();
            merge.include(fetchResult.getAdvertisedRef("HEAD"));
            merge.setCommit(input.createMergeCommit);
            merge.setSquash(input.squashCommit);
            merge.setFastForward(input.ffMode);
            merge.setStrategy(input.mergeStrategy);
            MergeResult mergeResult = merge.call();
        } catch (GitAPIException e) {
            System.out.println("GIT FAILURE");
        } catch (IOException e) {
            System.out.println("IO FAILURE");
        }
    }
}