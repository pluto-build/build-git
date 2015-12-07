package build.pluto.buildgit.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

import build.pluto.buildgit.GitException;
import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.bound.BranchBound;
import build.pluto.buildgit.bound.CommitHashBound;
import build.pluto.buildgit.bound.UpdateBound;

public class GitHandler {

    private static Git openRepository(File directory) {
        try {
            return Git.open(directory);
        } catch (IOException e) {
            return null;
        }
    }

    public static void cloneRepository(GitInput input) throws GitException {
        try {
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
            throw new GitException("Clone of repository " + input.url + " failed", e);
        }
    }

    public static void checkout(File directory, String hash) throws GitException {
        try {
            Git git = openRepository(directory);
            git.checkout()
               .setName(hash)
               .call();
        } catch (GitAPIException e) {
            throw new GitException("Checkout in directory " + directory + " failed", e);
        }
    }

    public static void pull(GitInput input) throws GitException {
        FetchResult fetchResult = fetch(input.directory, input.url);
    	Ref ref = fetchResult.getAdvertisedRef("HEAD");
        MergeResult mergeResult = merge(input, ref);
        if (!mergeResult.getMergeStatus().isSuccessful()) {
        	throw new GitException("Merge of " + ref + " in " + input.directory + " failed");
        }
    }

    private static FetchResult fetch(File directory, String url) throws GitException {
        try {
            String remoteOfUrl = getRemoteOfUrl(directory, url);
            Git git = openRepository(directory);
            return git.fetch()
                      .setRemote(remoteOfUrl)
                      .call();
        } catch (GitAPIException e) {
            throw new GitException("Fetch of repository " + url + " failed", e);
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

    private static MergeResult merge(GitInput input, Ref ref) throws GitException {
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
            throw new GitException("Merge of " + ref + " in " + input.directory + " failed" , e);
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
        if(git == null) {
            return false;
        }
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

    public static void resetRepoToCommit(File directory, String commitHash)
            throws InvalidRefNameException {
        try {
            Git git = openRepository(directory);
            git.reset()
               .setMode(ResetCommand.ResetType.HARD)
               .setRef(commitHash)
               .call();
        } catch (GitAPIException e) {
            throw new InvalidRefNameException("Ref  "
                    + commitHash + " does not exist");
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

    public static List<File> getNotIgnoredFilesOfRepo(File directory) {
        Git git = openRepository(directory);
        Repository repo = null;
        repo = git.getRepository();
        List<File> foundFiles = new ArrayList<>();
        TreeWalk treeWalk = null;
        try {
        	treeWalk = new TreeWalk(repo);
            FileTreeIterator tree = new FileTreeIterator(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            while (treeWalk.next()) {
                WorkingTreeIterator iterator = treeWalk.getTree(0, WorkingTreeIterator.class);
                if(!iterator.isEntryIgnored())
                    if (treeWalk.isSubtree()) {
                        treeWalk.enterSubtree();
                    } else {
                        File file = new File(directory, treeWalk.getPathString());
                        foundFiles.add(file);
                    }
            }
        } catch (Exception e) {
            return null;
        } finally {
        	if (treeWalk != null)
        		treeWalk.close();
        }
        return foundFiles;
    }
}
