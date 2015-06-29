package build.pluto.gitbuilder.util;

import build.pluto.gitbuilder.Input;
import build.pluto.gitbuilder.exception.NotClonedException;
import build.pluto.gitbuilder.exception.NotCheckedOutException;
import build.pluto.gitbuilder.exception.NotPulledException;
import build.pluto.gitbuilder.util.GitHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.sugarj.common.FileCommands;

public class GitHandlerTest {

    private Input in;
    private GitHandler tested;
    private String dummyPath;

    @Before
    public void init() {
        File dummyLocation = new File("src/test/resources/dummy");
        this.dummyPath = "file://" + dummyLocation.getAbsolutePath();
        this.in = this.createInput("test", this.dummyPath);
        this.tested = new GitHandler(in);
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
        assertTrue(tested.isUrlAccessible());
    }

    @Test
    public void checkNotIsUrlAccessible() {
        Input in = this.createInput("test", "https://github.com/andider/dummy.git");
        GitHandler tested = new GitHandler(in);
        assertFalse(tested.isUrlAccessible());
    }

    @Test
    public void checkClone() {
        this.clone(tested);
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
        this.clone(tested);
        assertTrue(tested.isUrlSet());
    }

    @Test
    public void checkIsUrlNotSet() {
        try {
            tested.cloneRepository();
            Repository repo = Git.open(in.directory).getRepository();
            StoredConfig config = repo.getConfig();
            config.unsetSection("remote", "origin");
            config.save();
            assertFalse(tested.isUrlSet());
        } catch (IOException e) {
            fail("Could not open repository");
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
    }

    @Test
    public void checkPull() throws InvalidRefNameException {
        this.clone(tested);
        try {
            String content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            tested.pull();
            content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            fail("Could not read file");
        } catch (NotPulledException e) {
            fail("Could not pull repository");
        }
    }

    @Test(expected = NotPulledException.class)
    public void checkPullWithLocalCommit() throws NotPulledException {
        this.clone(tested);
        try {
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            FileCommands.writeToFile(new File(in.directory, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
            Git.open(in.directory).add().addFilepattern("README.md").call();
            Git.open(in.directory).commit().setMessage("local changes").call();
        } catch (IOException e) {
            fail("Could not open the repository");
        } catch (GitAPIException e) {
            fail("Could not commit local change");
        }
        tested.pull();
    }

    @Test
    public void testCheckout() {
       this.clone(tested);
       Exception ex = null;
       try {
           tested.checkout("feature");
       } catch (NotCheckedOutException e ) {
           ex = e;
       }
       assertNull(ex);
    }

    @Test(expected = NotCheckedOutException.class)
    public void testCheckoutFailed() throws NotCheckedOutException {
        this.clone(tested);
        tested.checkout("master2");
    }

    @Test
    public void checkGetHashOfRemoteHEADMaster() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "master");
        assertEquals("ddfa2acb09533f16792f6006316ce2744792d839", s);
    }

    @Test
    public void checkGetHashOfRemoteHEADFeature() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "feature");
        assertEquals("c55e35f7b4d3ff14cb8a99268e6ae0439e6c0d6f", s);
    }

    private void clone(GitHandler handler) {
        try {
            handler.cloneRepository();
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
