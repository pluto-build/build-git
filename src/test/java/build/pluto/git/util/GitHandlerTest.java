package build.pluto.git.util;

import build.pluto.git.Input;
import build.pluto.git.bound.UpdateBound;
import build.pluto.git.bound.TagBound;
import build.pluto.git.bound.BranchBound;
import build.pluto.git.bound.CommitHashBound;
import build.pluto.git.exception.NotCheckedOutException;
import build.pluto.git.exception.NotClonedException;
import build.pluto.git.exception.NotPulledException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GitHandlerTest {

    private Input in;
    private String dummyPath;
    private final String featureHeadHash = "c55e35f7b4d3ff14cb8a99268e6ae0439e6c0d6f";
    private final String masterHeadHash = "ddfa2acb09533f16792f6006316ce2744792d839";
    private final String tagHash = "3d8913c40c2387488172368a5cf9cf5eb64fed88";

    @Before
    public void init() {
        File dummyLocation = new File("src/test/resources/dummy");
        this.dummyPath = "file://" + dummyLocation.getAbsolutePath();
        this.in = this.createInput("test", this.dummyPath);
    }

    @After
    public void destroy() {
        try {
            FileCommands.delete(in.directory);
        } catch (IOException e) {
            fail("Could not delete temporary directory");
        }
    }

    @Test
    public void checkIsUrlAccessible() {
        assertTrue(GitHandler.isUrlAccessible(in.url));
    }

    @Test
    public void checkNotIsUrlAccessible() {
        assertFalse(GitHandler.isUrlAccessible("deadlink"));
    }

    @Test
    public void checkClone() {
        clone(in);
        boolean fileExists = false;
        for (Path p : FileCommands.listFilesRecursive(in.directory.toPath())) {
            if (p.getFileName().toString().equals("README.md")) {
                fileExists = true;
            }
        }
        assertTrue(fileExists);
    }

    @Test
    public void checkIsUrlSet() {
        clone(in);
        assertTrue(GitHandler.isUrlSet(in.directory, in.url));
    }

    @Test
    public void checkIsUrlNotSet() {
        try {
            clone(in);
            Repository repo = Git.open(in.directory).getRepository();
            StoredConfig config = repo.getConfig();
            config.unsetSection("remote", "origin");
            config.save();
            assertFalse(GitHandler.isUrlSet(in.directory, in.url));
        } catch (IOException e) {
            fail("Could not open repository");
        }
    }

    @Test
    public void checkPull() throws InvalidRefNameException {
        try {
            clone(in);
            String content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            GitHandler.pull(in);
            content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            fail("Could not read file");
        } catch (NotPulledException e) {
            e.printStackTrace();
            fail("Could not pull repository");
        }
    }

    @Test(expected = NotPulledException.class)
    public void checkPullWithLocalCommit() throws NotPulledException {
        clone(in);
        try {
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            FileCommands.writeToFile(new File(in.directory, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
            GitHandler.add(in.directory, "README.md");
            GitHandler.commit(in.directory, "local changes");
        } catch (IOException e) {
            fail("Could not open the repository");
        } catch (GitAPIException e) {
            fail("Could not commit local change");
        }
        GitHandler.pull(in);
    }

    @Test
    public void testCheckout() {
        clone(in);
        Exception ex = null;
        try {
            GitHandler.checkout(in.directory, "feature");
        } catch (NotCheckedOutException e ) {
            ex = e;
        }
        assertNull(ex);
    }

    @Test(expected = NotCheckedOutException.class)
    public void testCheckoutFailed() throws NotCheckedOutException {
        clone(in);
        GitHandler.checkout(in.directory, "master2");
    }

    @Test
    public void checkGetHashOfRemoteHEADMaster() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "master");
        assertEquals(masterHeadHash, s);
    }

    @Test
    public void checkGetHashOfRemoteHEADFeature() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "feature");
        assertEquals(featureHeadHash, s);
    }

    @Test
    public void checkGetHashOfCommitHashBound(){
        UpdateBound bound = new CommitHashBound(masterHeadHash);
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(masterHeadHash, s);
    }

    @Test
    public void checkGetHashOfTagBound(){
        UpdateBound bound = new TagBound(dummyPath, "v0.1");
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(tagHash, s);
    }

    @Test
    public void checkGetHashOfBranchBound(){
        UpdateBound bound = new BranchBound(dummyPath, "feature");
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(featureHeadHash, s);
    }

    private void clone(Input input) {
        try {
            GitHandler.cloneRepository(input);
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
    }

    private Input createInput(String local, String remote) {
        File localFile = new File(local);
        Input.Builder inputBuilder = new Input.Builder(localFile, remote, null);
        inputBuilder.addBranchToClone("feature");
        return inputBuilder.build();
    }
}
