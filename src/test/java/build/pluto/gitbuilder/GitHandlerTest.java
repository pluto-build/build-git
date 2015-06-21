package build.pluto.gitbuilder;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHandlerTest {
    @Test
    public void checkIsUrlAccessible() {
        Input in = this.createInput("test6", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
        assertTrue(tested.isUrlAccessible());
    }

    @Test
    public void checkNotIsUrlAccessible() {
        Input in = this.createInput("test6", "https://github.com/andider/dummy.git");
        GitHandler tested = new GitHandler(in);
        assertFalse(tested.isUrlAccessible());
    }

    @Test
    public void checkClone() {
        Input in = this.createInput("test8", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
        this.deleteTempDir(in.directory);
        assertFalse(FileCommands.exists(in.directory));
        try {
            tested.cloneRepository();
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
        assertTrue(FileCommands.exists(in.directory));
        boolean fileExists = false;
        for (Path p : FileCommands.listFilesRecursive(in.directory.toPath())) {
            if (p.getFileName().toString().equals("README.md")) {
                fileExists = true;
            }
        }
        assertTrue(fileExists);
        deleteTempDir(in.directory);
    }

    @Test
    public void checkIsUrlSet() {
        Input in = this.createInput("test9", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
        try {
            tested.cloneRepository();
            assertTrue(tested.isUrlSet());
        } catch (NotClonedException e) {
            fail("could not clone repository");
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test
    public void checkIsUrlNotSet() {
        Input in = this.createInput("test10", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
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
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test
    public void checkPull() {
        Input in = this.createInput("test11", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
        this.deleteTempDir(in.directory);
        try {
            tested.cloneRepository();
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
        try {
            String content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            Git.open(in.directory).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
            content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing\n");
            try {
                tested.pull();
            } catch (NotPulledException e) {
                fail("Could not pull repository");
            }
            content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test(expected = NotPulledException.class)
    public void checkPullWithLocalCommit() throws NotPulledException {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git");
        GitHandler tested = new GitHandler(in);
        this.deleteTempDir(in.directory);
        try {
            tested.cloneRepository();
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
        try {
            Git.open(in.directory).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
            FileCommands.writeToFile(new File(in.directory, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
            Git.open(in.directory).add().addFilepattern("README.md").call();
            Git.open(in.directory).commit().setMessage("local changes").call();
        } catch (IOException e) {
            fail("");
        } catch (GitAPIException e) {
            fail("");
        }
        tested.pull();
        deleteTempDir(in.directory);
    }

    private Input createInput(String local, String remote) {
        File localFile = new File(local);
        Input.Builder inputBuilder = new Input.Builder(localFile, remote, null);
        return inputBuilder.build();
    }

    private void deleteTempDir(File location) {
        try {
            FileCommands.delete(location);
        } catch (IOException e) {
            fail("Could not delete temporary directory");
        }
    }
}
