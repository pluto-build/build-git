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
        Input in = this.createInput("test", "https://github.com/andiderp/dummy.git");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        File local = new File("test");
        try {
            FileCommands.createDir(local.toPath());
            assertTrue(b.directoryExists(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            this.deleteTempDir(local);
        }
    }

    @Test
    public void checkLocalExistsNot() {
        Input in = this.createInput("test2", "https://github.com/andiderp/dummy.git");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertFalse(b.directoryExists(in));
    }

    @Test
    public void checkIsLocalEmpty() {
        Input in = this.createInput("test3", null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.directory.toPath());
            assertTrue(b.directoryIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test
    public void checkIsLocalEmptyWithDotFile() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.directory.toPath());
            File dotFile = new File(in.directory, ".dot");
            FileCommands.createFile(dotFile);
            assertFalse(b.directoryIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test
    public void checkIsLocalNotEmpty() {
        Input in = this.createInput("test5", null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.directory.toPath());
            File file = new File(in.directory, "test.txt");
            FileCommands.createFile(file);
            assertFalse(b.directoryIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(in.directory);
        }
    }

    @Test
    public void checkContainsFile() {
        Input in = this.createInput("test6", null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);

        File test6 = new File("test6");
        File test = new File(test6, ".test");
        File testFile = new File(test6, "test.txt");
        File testFile2 = new File(test, "test.txt");

        try {
            FileCommands.createDir(test6.toPath());
            FileCommands.createDir(test.toPath());
            FileCommands.createFile(testFile);
            FileCommands.createFile(testFile2);
            assertTrue(b.containsFile(test6, testFile));
            assertTrue(b.containsFile(test6, testFile2));
            assertFalse(b.containsFile(test, testFile));
            assertFalse(b.containsFile(test, test6));
            assertTrue(b.containsFile(test, testFile2));
            assertTrue(b.containsFile(test6, test));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            deleteTempDir(test6);
            deleteTempDir(test);
        }
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