package build.pluto.gitbuilder.util;

import build.pluto.gitbuilder.FastForwardMode;
import build.pluto.gitbuilder.Input;
import build.pluto.gitbuilder.bound.UpdateBound;
import build.pluto.gitbuilder.bound.CommitHashBound;
import build.pluto.gitbuilder.bound.BranchBound;
import build.pluto.gitbuilder.exception.NotCheckedOutException;
import build.pluto.gitbuilder.exception.NotClonedException;
import build.pluto.gitbuilder.exception.NotFetchedException;
import build.pluto.gitbuilder.exception.NotPulledException;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GitHandler {

    private Git git;
    private Input input;

    public GitHandler(Input input) {
        try {
            this.git = openRepository(input.directory);
        } catch(Exception e) {
            this.git = null;
        }
        this.input = input;
    }

    private static Git openRepository(File directory) throws Exception {
            try {
                return Git.open(directory);
            } catch (IOException e) {
                throw new Exception();
            }
    }

    public void cloneRepository() throws NotClonedException {
        try {
            List<String> branchesToClone = new ArrayList<>();
            this.git = Git.cloneRepository()
                    .setURI(input.url)
                    .setDirectory(input.directory)
                    .setCloneSubmodules(input.cloneSubmodules)
                    .call();
            for(String branchName : input.branchesToClone) {
                this.git.checkout()
                        .setCreateBranch(true)
                        .setName(branchName)
                        .setStartPoint("origin/" + branchName).call();
            }
            this.git.checkout().setName("master").call();
        } catch (GitAPIException e) {
            this.git = null;
            e.printStackTrace();
            throw new NotClonedException();
        }
    }

    public void checkout(String hash) throws NotCheckedOutException {
        try {
            git.checkout()
               .setName(hash)
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
        try {
            String url = getRemoteOfUrl();
            FetchCommand fetch = git.fetch()
                                    .setRemote(url);
            return fetch.call();
        } catch (GitAPIException e) {
            throw new NotFetchedException();
        }
    }

    private String getRemoteOfUrl() {
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

    private MergeResult merge(Ref ref) throws NotMergedException {
        try {
            MergeCommand merge = git.merge();
            merge.include(ref);
            merge.setCommit(input.createMergeCommit);
            merge.setSquash(input.squashCommit);
            merge.setFastForward(getFastForwardMode());
            merge.setStrategy(getMergeStrategy());
            return merge.call();
        } catch (GitAPIException e) {
            throw new NotMergedException();
        }
    }

    private MergeCommand.FastForwardMode getFastForwardMode() {
        if (input.ffMode == FastForwardMode.FF_ONLY) {
            return MergeCommand.FastForwardMode.FF_ONLY;
        } else if (input.ffMode == FastForwardMode.FF) {
            return MergeCommand.FastForwardMode.FF;
        } else {
            return MergeCommand.FastForwardMode.NO_FF;
        }
    }

    private org.eclipse.jgit.merge.MergeStrategy getMergeStrategy() {
        if (input.mergeStrategy == build.pluto.gitbuilder.MergeStrategy.OURS) {
            return org.eclipse.jgit.merge.MergeStrategy.OURS;
        } else if (input.mergeStrategy == build.pluto.gitbuilder.MergeStrategy.RECURSIVE) {
            return org.eclipse.jgit.merge.MergeStrategy.RECURSIVE;
        } else if (input.mergeStrategy == build.pluto.gitbuilder.MergeStrategy.RESOLVE) {
            return org.eclipse.jgit.merge.MergeStrategy.RESOLVE;
        } else if (input.mergeStrategy ==build.pluto.gitbuilder.MergeStrategy.SIMPLE_TWO_WAY_IN_CORE) {
            return org.eclipse.jgit.merge.MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;
        } else {
            return org.eclipse.jgit.merge.MergeStrategy.THEIRS;
        }
    }

    public boolean isUrlSet() {
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

    public static void resetRepoToCommit(File directory, String commitHash) throws InvalidRefNameException {
        try {
            Git.open(directory).reset().setMode(ResetCommand.ResetType.HARD).setRef(commitHash).call();
        } catch (IOException e) {
        } catch (GitAPIException e) {
            throw new InvalidRefNameException("Ref  " + commitHash + " does not exist");
        }
    }

    public static String getHashOfRemoteHEAD(String url, String branch) {
        BranchBound bound = new BranchBound(url, branch);
        return getHashOfBound(url, bound);
    }

    public static String getHashOfBound(String url, UpdateBound bound) {
        if (bound instanceof CommitHashBound) {
            return bound.getBoundHash();
        }
        String refName = bound.getBound();
        try {
            Collection<Ref> refs = Git.lsRemoteRepository().setRemote(url).setHeads(true).setTags(true).call();
            for (Ref ref : refs) {
                boolean isCorrectRef = ref.getName().contains(refName);
                if (isCorrectRef) {
                    ObjectId objectId = ref.getObjectId();
                    return ObjectId.toString(objectId);
                }
            }
        } catch (GitAPIException e) {
            return null;
        }
        return null;
    }
}
