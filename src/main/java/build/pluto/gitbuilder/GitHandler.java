package build.pluto.gitbuilder;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class GitHandler {

    private Git git;
    private Input input;

    public GitHandler(Input input) {
        this.git = null;
        this.input = input;
    }

    private Git openRepository() {
        if (this.git != null) {
            try {
                this.git = Git.open(input.local);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.git;
    }

    public void cloneRepository() throws NotClonedException {
        try {
            this.git = Git.cloneRepository()
                    .setURI(input.remote)
                    .setDirectory(input.local)
                    .setBranch(input.branchName)
                    .call();
        } catch (GitAPIException e) {
            this.git = null;
            throw new NotClonedException();
        }
    }

    public void pull() throws NotPulledException {
        FetchResult fetchResult = null;
        try {
            fetchResult = fetch();
        } catch (NotFetchedException e) {
            throw new NotPulledException();
        }
        try {
            MergeResult mergeResult = merge(fetchResult.getAdvertisedRef("HEAD"));
            if (!mergeResult.getMergeStatus().isSuccessful()) {
                throw new NotPulledException();
            }
        } catch (NotMergedException e) {
            throw new NotPulledException();
        }
    }

    private FetchResult fetch() throws NotFetchedException {
        openRepository();
        try {
            FetchCommand fetch = git.fetch();
            return fetch.call();
        } catch (GitAPIException e) {
            throw new NotFetchedException();
        }
    }

    private MergeResult merge(Ref ref) throws NotMergedException {
        openRepository();
        try {
            MergeCommand merge = git.merge();
            merge.include(ref);
            merge.setCommit(input.createMergeCommit);
            merge.setSquash(input.squashCommit);
            merge.setFastForward(input.ffMode);
            merge.setStrategy(input.mergeStrategy);
            return merge.call();
        } catch (GitAPIException e) {
            throw new NotMergedException();
        }
    }

    public boolean isRemoteSet() {
        openRepository();
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        boolean foundRemote = false;
        for (String remote : remotes) {
            String url = config.getString("remote", remote, "url");
            if (url.equals(input.remote)) {
                foundRemote = true;
            }
        }
        return foundRemote;
    }

    public boolean isRemoteAccessible() {
        try {
            Git.lsRemoteRepository().setRemote(input.remote).call();
        } catch (GitAPIException e) {
            return false;
        }
        return true;
    }

    public static boolean isRepo(File location) {
        try {
            Git.open(location);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
