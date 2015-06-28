package build.pluto.gitbuilder;

import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.gitbuilder.util.GitHandler;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.After;

import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import org.junit.Test;

public class GitRepositoryBuilderTest extends ScopedBuildTest {

    @ScopedPath("")
    private File directory;
    private File remoteLocation;

    @Before
    public void init() {
        this.remoteLocation = new File("src/test/resources/dummy");
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
    public void testNeedToPull() throws IOException {
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

    private TrackingBuildManager build() throws IOException {
        File binaryPath = null;
        TrackingBuildManager manager = new TrackingBuildManager();
        File summaryLocation = new File(directory, "temp");
        Input.Builder inputBuilder = new Input.Builder(directory, "file://" + remoteLocation.getAbsolutePath(), summaryLocation);
        inputBuilder.setBranchName("master");
        inputBuilder.addBranchToClone("master2");
        inputBuilder.addBranchToClone("feature");
        Input input = inputBuilder.build();
        manager.require(GitRepositoryBuilder.factory, input);
        return manager;
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

    private void deleteTempCommitOnRemote() {
        try {
            Git.open(remoteLocation).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
        } catch (IOException e) {
            fail("Could not open remote repository");
        } catch (GitAPIException e) {
            fail("Could not delete temporary commit");
        }
    }

    private void assertCorrectHead(String hash) {
        String commitHash = GitHandler.getHashOfRemoteHEAD("file://" + directory.getAbsolutePath(), "master");
        assertEquals(hash, commitHash);
    }
}
