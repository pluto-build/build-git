package build.pluto.git;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.git.bound.BranchBound;
import build.pluto.git.bound.CommitHashBound;
import build.pluto.git.bound.TagBound;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.util.GitHandler;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GitRemoteSynchronizerTest extends ScopedBuildTest {

    @ScopedPath("")
    private File directory;
    private File remoteLocation;
    private UpdateBound bound;
    private long consistencyCheckInterval;

    private final String featureHeadHash = "c55e35f7b4d3ff14cb8a99268e6ae0439e6c0d6f";
    private final String masterHeadHash = "ddfa2acb09533f16792f6006316ce2744792d839";
    private final String master2HeadHash = "99417aa270f38d6a7d5aef584570653f58eef14b";
    private final String tagHash = "3d8913c40c2387488172368a5cf9cf5eb64fed88";

    @Before
    public void init() {
        this.remoteLocation = new File("src/test/resources/dummy");
        this.bound = new BranchBound(getPathOfRemote(), "master");
        this.consistencyCheckInterval = 0;
    }

    @Test
    public void testDirectoryEmpty() throws IOException {
        build();
        assertCorrectHead(masterHeadHash);
    }

    @Test(expected = RequiredBuilderFailed.class)
    public void testDirectoryNotEmpty() throws IOException {
        File tempFile = new File(directory, "OK.txt");
        FileCommands.createFile(tempFile);
        build();
    }

    @Test
    public void testCleanRebuildDoesNothing() throws IOException {
        build();
        build();
        assertCorrectHead(masterHeadHash);
    }

    @Test
    public void testNeedToPull() throws IOException, InvalidRefNameException {
        build();
        createCommitOnRemote();
        String newHEADHashOfRemote = GitHandler.getHashOfRemoteHEAD(getPathOfRemote(), "master");
        build();
        assertCorrectHead(newHEADHashOfRemote);
        deleteTempCommitOnRemote();
    }

    @Test
    public void testDontCheckConsistencyToEarly() throws IOException, InvalidRefNameException {
        this.consistencyCheckInterval = (long) 10e6;
        build();
        createCommitOnRemote();
        String newHEADHashOfRemote = GitHandler.getHashOfRemoteHEAD(getPathOfRemote(), "master");
        build();
        assertCorrectHead(masterHeadHash);
        deleteTempCommitOnRemote();
    }

    @Test
    public void testPullAfterCheckout() throws Exception {
        build();
        createCommitOnRemote();
        String newHEADHashOfRemote = GitHandler.getHashOfRemoteHEAD(getPathOfRemote(), "master");
        GitHandler.checkout(directory, "feature");
        assertCorrectHead(featureHeadHash);
        build();
        assertCorrectHead(newHEADHashOfRemote);
        deleteTempCommitOnRemote();
    }

    @Test
    public void testBuildAfterCheckout() throws Exception {
        build();
        GitHandler.checkout(directory, "feature");
        assertCorrectHead(featureHeadHash);
        build();
        assertCorrectHead(masterHeadHash);
    }

    @Test(expected = RequiredBuilderFailed.class)
    public void testRemoteNotAccessible() throws IOException {
        remoteLocation = new File("deadlink");
        build();
    }

    @Test
    public void testCommitBoundCurrentHEADAfterClone() throws IOException {
        this.bound = new CommitHashBound(masterHeadHash);
        build();
        assertCorrectHead(masterHeadHash);
    }

    @Test
    public void testCommitBoundCurrentHEADAfterPull() throws IOException {
        this.bound = new CommitHashBound(masterHeadHash);
        build();
        build();
        assertCorrectHead(masterHeadHash);
    }

    @Test
    public void testTagBoundCurrentHEADAfterClone() throws IOException {
        this.bound = new TagBound(getPathOfRemote(), "v0.1");
        build();
        assertCorrectHead(tagHash);
    }

    @Test
    public void testTagBoundCurrentHEADAfterPull() throws IOException {
        this.bound = new TagBound(getPathOfRemote(), "v0.1");
        build();
        build();
        assertCorrectHead(tagHash);
    }

    @Test
    public void testBranchBoundCurrentHEADAfterClone() throws IOException {
        this.bound = new BranchBound(getPathOfRemote(), "master2");
        build();
        assertCorrectHead(master2HeadHash);
    }

    @Test
    public void testBranchBoundCurrentHEADAfterPull() throws IOException {
        this.bound = new BranchBound(getPathOfRemote(), "master2");
        build();
        build();
        assertCorrectHead(master2HeadHash);
    }

    private void build() throws IOException {
        File summaryLocation = new File(directory, "temp");
        GitInput.Builder inputBuilder = new GitInput.Builder(directory, getPathOfRemote(), summaryLocation);
        inputBuilder.addBranchToClone("master2");
        inputBuilder.addBranchToClone("feature");
        inputBuilder.setBound(this.bound);
        inputBuilder.setConsistencyCheckInterval(this.consistencyCheckInterval);
        GitInput input = inputBuilder.build();
        System.out.println("BUILD");
        BuildRequest<?, ?, ?, ?> buildRequest = new BuildRequest(GitRemoteSynchronizer.factory, input);
        BuildManagers.build(buildRequest);
    }

    private String getPathOfRemote() {
        return "file://" + this.remoteLocation.getAbsolutePath();
    }

    private void assertCorrectHead(String hash) {
        try{
            String commitHash = GitHandler.getHashOfHEAD(directory);
            assertEquals(hash, commitHash);
        } catch (Exception e) {
            fail("Could not read hash of HEAD");
        }
    }

    private void createCommitOnRemote() {
        File newFile = new File(remoteLocation, "tempChange.txt");
        try {
            FileCommands.createFile(newFile);
            FileCommands.writeToFile(newFile, "TEMP");
            Git.open(remoteLocation).add().addFilepattern("tempChange.txt").call();
            Git.open(remoteLocation).commit().setMessage("temp commit").call();
        } catch (IOException e) {
            fail("Could not open remote repository");
        } catch (GitAPIException e) {
            fail("Could not create temporary commit");
        }
    }

    private void deleteTempCommitOnRemote() throws InvalidRefNameException {
        GitHandler.resetRepoToCommit(remoteLocation, masterHeadHash);
    }
}
