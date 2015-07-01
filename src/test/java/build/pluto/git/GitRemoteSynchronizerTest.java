package build.pluto.git;

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

    @Before
    public void init() {
        this.remoteLocation = new File("src/test/resources/dummy");
        this.bound = new BranchBound("file://" + remoteLocation.getAbsolutePath(), "master");
    }

    @Test
    public void testDirectoryEmpty() throws IOException {
        build();
        assertCorrectHead("ddfa2acb09533f16792f6006316ce2744792d839");
    }

    @Test(expected = RequiredBuilderFailed.class)
    public void testDirectoryNotEmpty() throws IOException {
        File tempFile = new File(directory, "OK.txt");
        FileCommands.createDir(tempFile.toPath());
        build();
    }

    @Test
    public void testCleanRebuildDoesNothing() throws IOException {
        build();
        build();
        assertCorrectHead("ddfa2acb09533f16792f6006316ce2744792d839");
    }

    @Test
    public void testNeedToPull() throws IOException, InvalidRefNameException {
        build();
        createCommitOnRemote();
        String newHEADHashOfRemote = GitHandler.getHashOfRemoteHEAD("file://" + remoteLocation.getAbsolutePath(), "master");
        build();
        assertCorrectHead(newHEADHashOfRemote);
        deleteTempCommitOnRemote();
    }

    @Test(expected = RequiredBuilderFailed.class)
    public void testRemoteNotAccessible() throws IOException {
        remoteLocation = new File("deadlink");
        build();
    }

    @Test
    public void testCommitBoundCurrentHEADAfterClone() throws IOException {
        String commitHash = "99417aa270f38d6a7d5aef584570653f58eef14b";
        this.bound = new CommitHashBound(commitHash);
        build();
        assertCorrectHead(commitHash);
    }

    @Test
    public void testCommitBoundCurrentHEADAfterPull() throws IOException {
        String commitHash = "99417aa270f38d6a7d5aef584570653f58eef14b";
        this.bound = new CommitHashBound(commitHash);
        build();
        build();
        assertCorrectHead(commitHash);
    }

    @Test
    public void testTagBoundCurrentHEADAfterClone() throws IOException {
        this.bound = new TagBound("v0.1");
        build();
        assertCorrectHead("3d8913c40c2387488172368a5cf9cf5eb64fed88");
    }

    @Test
    public void testTagBoundCurrentHEADAfterPull() throws IOException {
        this.bound = new TagBound("v0.1");
        build();
        build();
        assertCorrectHead("3d8913c40c2387488172368a5cf9cf5eb64fed88");
    }

    @Test
    public void testBranchBoundCurrentHEADAfterClone() throws IOException {
        this.bound = new BranchBound("file://"+ this.remoteLocation.getAbsolutePath(), "master2");
        build();
        assertCorrectHead("99417aa270f38d6a7d5aef584570653f58eef14b");
    }

    @Test
    public void testBranchBoundCurrentHEADAfterPull() throws IOException {
        this.bound = new BranchBound("file://"+ this.remoteLocation.getAbsolutePath(), "master2");
        build();
        build();
        assertCorrectHead("99417aa270f38d6a7d5aef584570653f58eef14b");
    }

    private TrackingBuildManager build() throws IOException {
        File binaryPath = null;
        TrackingBuildManager manager = new TrackingBuildManager();
        File summaryLocation = new File(directory, "temp");
        Input.Builder inputBuilder = new Input.Builder(directory, "file://" + remoteLocation.getAbsolutePath(), summaryLocation);
        inputBuilder.addBranchToClone("master2");
        inputBuilder.addBranchToClone("feature");
        inputBuilder.setBound(this.bound);
        Input input = inputBuilder.build();
        manager.require(GitRemoteSynchronizer.factory, input);
        return manager;
    }

    private void assertCorrectHead(String hash) {
        String commitHash = GitHandler.getHashOfRemoteHEAD("file://" + directory.getAbsolutePath(), "master");
        assertEquals(hash, commitHash);
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
        GitHandler.resetRepoToCommit(remoteLocation, "ddfa2acb09533f16792f6006316ce2744792d839");
    }
}
