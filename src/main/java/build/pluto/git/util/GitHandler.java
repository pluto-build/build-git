package build.pluto.git.util;

import build.pluto.git.Input;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.bound.CommitHashBound;
import build.pluto.git.bound.BranchBound;
import build.pluto.git.exception.NotCheckedOutException;
import build.pluto.git.exception.NotClonedException;
import build.pluto.git.exception.NotFetchedException;
import build.pluto.git.exception.NotPulledException;
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

    private static Git openRepository(File directory) {
        try {
            return Git.open(directory);
        } catch (IOException e) {
            return null;
        }
    }

    public static void cloneRepository(Input input) throws NotClonedException {
        try {
            List<String> branchesToClone = new ArrayList<>();
            Git git = Git.cloneRepository()
                    .setURI(input.url)
                    .setDirectory(input.directory)
                    .setCloneSubmodules(input.cloneSubmodules)
                    .call();
            for(String branchName : input.branchesToClone) {
                git.checkout()
                   .setCreateBranch(true)
                   .setName(branchName)
                   .setStartPoint("origin/" + branchName).call();
            }
            git.checkout().setName("master").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new NotClonedException();
        }
    }

    public static void checkout(File directory, String hash) throws NotCheckedOutException {
        try {
            Git git = openRepository(directory);
            git.checkout()
               .setName(hash)
               .call();
        } catch (GitAPIException e) {
            throw new NotCheckedOutException();
        }
    }

    public static void pull(Input input) throws NotPulledException {
        FetchResult fetchResult = null;
        try {
            fetchResult = fetch(input.directory, input.url);
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

    private static FetchResult fetch(File directory, String url) throws NotFetchedException {
        try {
            String remoteOfUrl = getRemoteOfUrl(directory, url);
            Git git = openRepository(directory);
            return git.fetch()
                      .setRemote(remoteOfUrl)
                      .call();
        } catch (GitAPIException e) {
            throw new NotFetchedException();
        }
    }

    private static String getRemoteOfUrl(File directory, String url) {
        Git git = openRepository(directory);
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        for (String remote : remotes) {
            String remoteUrl = config.getString("remote", remote, "url");
            if(remoteUrl.equals(url)) {
                return remote;
            }
        }
        return null;
    }

    private static MergeResult merge(Input input, Ref ref) throws NotMergedException {
        try {
            Git git = openRepository(input.directory);
            MergeCommand merge = git.merge();
            merge.include(ref);
            merge.setCommit(input.createMergeCommit);
            merge.setSquash(input.squashCommit);
            merge.setFastForward(input.ffMode.getMode());
            merge.setStrategy(input.mergeStrategy.getStrategy());
            return merge.call();
        } catch (GitAPIException e) {
            throw new NotMergedException();
        }
    }

    public static void add(File directory, String filePattern) {
        try {
            Git git = openRepository(directory);
            git.add().addFilepattern(filePattern).call();
        } catch (GitAPIException e ) {
        }
    }

    public static void commit(File directory, String message) {
        try {
            Git git = openRepository(directory);
            git.commit().setMessage(message).call();
        } catch (GitAPIException e ) {
        }
    }

    public static boolean isUrlSet(File directory, String url) {
        Git git = openRepository(directory);
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        boolean foundRemote = false;
        for (String remote : remotes) {
            String configUrl = config.getString("remote", remote, "url");
            if (configUrl.equals(url)) {
                foundRemote = true;
            }
        }
        return foundRemote;
    }

    public static boolean isUrlAccessible(String url) {
        try {
            Git.lsRemoteRepository().setRemote(url).call();
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
            Git git = openRepository(directory);
            git.reset()
               .setMode(ResetCommand.ResetType.HARD)
               .setRef(commitHash)
               .call();
        } catch (GitAPIException e) {
            throw new InvalidRefNameException("Ref  " + commitHash + " does not exist");
        }
    }

    public static String getHashOfHEAD(File directory) throws IOException {
        Git git = openRepository(directory);
        Ref headRef = git.getRepository().getRef("HEAD");
        ObjectId objectId = headRef.getObjectId();
        return ObjectId.toString(objectId);
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
            Collection<Ref> refs = Git.lsRemoteRepository()
                                      .setRemote(url)
                                      .setHeads(true)
                                      .setTags(true)
                                      .call();
            for (Ref ref : refs) {
                boolean isCorrectRef = ref.getName().contains(refName);
                if (isCorrectRef) {
                    ObjectId objectId = ref.getPeeledObjectId();
                    if (objectId == null) {
                        objectId = ref.getObjectId();
                    }
                    return ObjectId.toString(objectId);
                }
            }
        } catch (GitAPIException e) {
            return null;
        }
        return null;
    }
}