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
        return "Keeps the directory " + in.localLocation.toString() + " in sync with " + in.remoteLocation;
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
        if (!localLocationExists(in) || localLocationIsEmpty(in)) {
            if (isRemoteLocationAccessible(in)) {
                cloneRepository(in);
            } else {
                throw new TransportException(in.remoteLocation + " can not be accessed");
            }
        } else {
            if (isLocalLocationRepo(in)) {
                if (isRemoteLocationAccessible(in) && isRemoteSet(in)) {
                    pullRepository(in);
                } else {
                    //do nothing
                }
            } else {
                throw new IllegalArgumentException(in.localLocation.toString() + " is not empty and does contains other data than the repository");
            }
        }

        //TODO: maybe only provide files not ignored via .gitignore
        List<Path> outputFiles = FileCommands.listFilesRecursive(in.localLocation.toPath());
        for (Path p : outputFiles) {
            if (!p.toAbsolutePath().toString().contains(".git")) {
                provide(p.toFile());
            }
        }
        return None.val;
    }

    public boolean localLocationExists(Input in) {
        return FileCommands.exists(in.localLocation);
    }

    public boolean localLocationIsEmpty(Input in) {
        return FileCommands.listFilesRecursive(in.localLocation.toPath()).size() == 0;
    }

    public boolean isLocalLocationRepo(Input in) {
        try {
            Git.open(in.localLocation);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean isRemoteSet(Input input) {
        try {
            StoredConfig config = Git.open(input.localLocation).getRepository().getConfig();
            Set<String> remotes = config.getSubsections("remote");
            boolean foundRemote = false;
            for (String remote : remotes) {
                String url = config.getString("remote", remote, "url");
                if (url.equals(input.remoteLocation)) {
                    foundRemote = true;
                }
            }
            return foundRemote;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isRemoteLocationAccessible(Input in) {
        try {
            Git.lsRemoteRepository().setRemote(in.remoteLocation).call();
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
            Git g = Git.open(in.localLocation);

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
                    .setURI(in.remoteLocation)
                    .setDirectory(in.localLocation)
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