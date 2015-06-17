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
        if (!localExists(input) || localIsEmpty(input)) {
            if (isRemoteAccessible(input)) {
                clone(input);
            } else {
                throw new TransportException(input.remote + " can not be accessed");
            }
        } else {
            if (isLocalRepo(input)) {
                if (isRemoteAccessible(input) && isRemoteSet(input)) {
                    pull(input);
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
        } catch (GitAPIException e) {
            return false;
        }
        return true;
    }

    public void clone(Input input) throws NotClonedException {
        try {
            Git result = Git.cloneRepository()
                            .setURI(input.remote)
                            .setDirectory(input.local)
                            .setBranch(input.branchName)
                            .call();
        } catch (GitAPIException e) {
            throw new NotClonedException();
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

    public void pull(Input input) throws NotPulledException {
        FetchResult fetchResult = null;
        try {
            fetchResult = fetch(input);
        } catch (NotFetchedException e) {
            throw new NotPulledException();
        }
        try {
            MergeResult mergeResult = merge(input, fetchResult.getAdvertisedRef("HEAD"));
            if (!mergeResult.getMergeStatus().isSuccessful()) {
                throw new NotPulledException();
            }
        } catch (NotMergedException e) {
            throw new NotPulledException();
        }
    }

    public FetchResult fetch(Input input) throws NotFetchedException {
        try {
            Git g = Git.open(input.local);
            FetchCommand fetch = g.fetch();
            return fetch.call();
        } catch (GitAPIException e) {
            throw new NotFetchedException();
        } catch (IOException e) {
            throw new NotFetchedException();
        }
    }

    public MergeResult merge(Input input, Ref ref) throws NotMergedException {
        try {
            Git g = Git.open(input.local);
            MergeCommand merge = g.merge();
            merge.include(ref);
            merge.setCommit(input.createMergeCommit);
            merge.setSquash(input.squashCommit);
            merge.setFastForward(input.ffMode);
            merge.setStrategy(input.mergeStrategy);
            return merge.call();
        } catch (IOException e) {
            throw new NotMergedException();
        } catch (GitAPIException e) {
            throw new NotMergedException();
        }
    }
}