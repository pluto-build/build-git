package build.pluto.gitbuilder;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
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
                this.git = Git.open(input.directory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.git;
    }

    public void cloneRepository() throws NotClonedException {
        try {
            this.git = Git.cloneRepository()
                    .setURI(input.url)
                    .setDirectory(input.directory)
                    .setBranch(input.branchName)
                    .call();
        } catch (GitAPIException e) {
            this.git = null;
            throw new NotClonedException();
        }
    }

    public void checkout(String branchName) throws NotCheckedOutException {
        openRepository();
        try {
            git.checkout()
               .setName(branchName)
               .call();
        } catch (GitAPIException e) {
            throw new NotCheckedOutException();
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
            String url = getRemoteOfUrl();
            FetchCommand fetch = git.fetch()
                                    .setRemote(url);
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

    private String getRemoteOfUrl() {
        openRepository();
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        for (String remote : remotes) {
            String url = config.getString("remote", remote, "url");
            if(url.equals(input.url)) {
                return remote;
            }
        }
        return null;
    }
    public boolean isUrlSet() {
        openRepository();
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        boolean foundRemote = false;
        for (String remote : remotes) {
            String url = config.getString("remote", remote, "url");
            if (url.equals(input.url)) {
                foundRemote = true;
            }
        }
        return foundRemote;
    }

    public boolean isUrlAccessible() {
        try {
            Git.lsRemoteRepository().setRemote(input.url).call();
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

    public static String getHashOfRemoteHEAD(String url, String branch) {
        try {
            Collection<Ref> refs = Git.lsRemoteRepository().setRemote(url).setHeads(true).setTags(false).call();
            for (Ref ref : refs) {
                boolean isRefOfBranch = ref.getName().contains(branch);
                if (isRefOfBranch) {
                    ObjectId objectId = ref.getObjectId();
                    return ObjectId.toString(objectId);
                }
            }
        } catch (GitAPIException e) {
            //TODO:throw exception
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
