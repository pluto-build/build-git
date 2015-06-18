package build.pluto.gitbuilder;

import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class GitRepositoryBuilderTest {

    @Test
    public void checkLocalExists() {
        Input in = this.createInput("test", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        File local = new File("test");
        try {
            FileCommands.createDir(local.toPath());
            assertTrue(b.localExists(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            this.deleteTempDir(local);
        }
    }

    @Test
    public void checkLocalExistsNot() {
        Input in = this.createInput("test2", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertFalse(b.localExists(in));
    }

    @Test
    public void checkIsLocalEmpty() {
        Input in = this.createInput("test3", null, null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.local.toPath());
            assertTrue(b.localIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.local);
        }
    }

    @Test
    public void checkIsLocalEmptyWithDotFile() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.local.toPath());
            File dotFile = new File(in.local, ".vimrc");
            FileCommands.createFile(dotFile);
            assertFalse(b.localIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.local);
        }
    }

    @Test
    public void checkIsLocalNotEmpty() {
        Input in = this.createInput("test5", null, null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.local.toPath());
            File file = new File(in.local, "test.txt");
            FileCommands.createFile(file);
            assertFalse(b.localIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.local);
        }
    }

    private Input createInput(String local, String remote, String branch) {
        File localFile = new File(local);
        Input.Builder inputBuilder = new Input.Builder(localFile, remote, branch, null);
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