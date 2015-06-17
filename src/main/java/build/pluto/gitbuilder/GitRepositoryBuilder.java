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
    protected String description(Input in) {
        return "Keeps the directory " + in.local.toString() + " in sync with " + in.remote;
    }

    @Override
    protected File persistentPath(Input in) {
        if (in.summaryLocation != null) {
            return new File(in.summaryLocation, "git.dep");
        }
        return new File("./git.dep");
    }

    @Override
    protected None build(Input in) throws Throwable {
        //TODO: think I don't need to require any files
        if (!localExists(in) || localIsEmpty(in)) {
            if (isRemoteAccessible(in)) {
                cloneRepository(in);
            } else {
                throw new TransportException(in.remote + " can not be accessed");
            }
        } else {
            if (isLocalRepo(in)) {
                if (isRemoteAccessible(in) && isRemoteSet(in)) {
                    pullRepository(in);
                } else {
                    //do nothing
                }
            } else {
                throw new IllegalArgumentException(in.local.toString() + " is not empty and does contains other data than the repository");
            }
        }

        //TODO: maybe only provide files not ignored by .gitignore
        List<Path> outputFiles = FileCommands.listFilesRecursive(in.local.toPath());
        for (Path p : outputFiles) {
            if (!p.toAbsolutePath().toString().contains(".git")) {
                provide(p.toFile());
            }
        }
        return None.val;
    }

    public boolean localExists(Input in) {
        return FileCommands.exists(in.local);
    }

    public boolean localIsEmpty(Input in) {
        return FileCommands.listFilesRecursive(in.local.toPath()).size() == 0;
    }

    public boolean isLocalRepo(Input in) {
        try {
            Git.open(in.local);
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

    public boolean isRemoteAccessible(Input in) {
        try {
            Git.lsRemoteRepository().setRemote(in.remote).call();
        } catch (InvalidRemoteException e) {
            return false;
        } catch (TransportException e) {
            return false;
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void pullRepository(Input in) {
        try {
            Git g = Git.open(in.local);

            FetchCommand fetch = g.fetch();
            FetchResult fetchResult = fetch.call();

            MergeCommand merge = g.merge();
            merge.include(fetchResult.getAdvertisedRef("HEAD"));
            merge.setCommit(in.createMergeCommit);
            merge.setSquash(in.squashCommit);
            merge.setFastForward(in.ffMode);
            merge.setStrategy(in.mergeStrategy);
            MergeResult mergeResult = merge.call();
        } catch (GitAPIException e) {
            System.out.println("GIT FAILURE");
        } catch (IOException e) {
            System.out.println("IO FAILURE");
        }
    }

    public void cloneRepository(Input in) {
        try {
            Git result = Git.cloneRepository()
                    .setURI(in.remote)
                    .setDirectory(in.local)
                    .setBranch(in.branchName)
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
}