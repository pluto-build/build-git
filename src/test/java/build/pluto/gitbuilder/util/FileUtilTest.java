package build.pluto.gitbuilder.util;

import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileUtilTest {

    @Test
    public void checkDirectoryIsEmpty() {
        File directory = new File("test3");
        try {
            FileCommands.createDir(directory.toPath());
            assertTrue(FileUtil.directoryIsEmpty(directory));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(directory);
        }
    }

    @Test
    public void checkDirectoryContainsDotFile() {
        File directory = new File("test4");
        try {
            FileCommands.createDir(directory.toPath());
            File dotFile = new File(directory, ".dot");
            FileCommands.createFile(dotFile);
            assertFalse(FileUtil.directoryIsEmpty(directory));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(directory);
        }
    }

    @Test
    public void checkDirectoryIsNotEmpty() {
        File directory = new File("test5");
        try {
            FileCommands.createDir(directory.toPath());
            File file = new File(directory, "test.txt");
            FileCommands.createFile(file);
            assertFalse(FileUtil.directoryIsEmpty(directory));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        } finally {
            deleteTempDir(directory);
        }
    }

    @Test
    public void checkContainsFile() {

        File test6 = new File("test6");
        File test = new File(test6, ".test");
        File testFile = new File(test6, "test.txt");
        File testFile2 = new File(test, "test.txt");

        try {
            FileCommands.createDir(test6.toPath());
            FileCommands.createDir(test.toPath());
            FileCommands.createFile(testFile);
            FileCommands.createFile(testFile2);
            assertTrue(FileUtil.containsFile(test6, testFile));
            assertTrue(FileUtil.containsFile(test6, testFile2));
            assertFalse(FileUtil.containsFile(test, testFile));
            assertFalse(FileUtil.containsFile(test, test6));
            assertTrue(FileUtil.containsFile(test, testFile2));
            assertTrue(FileUtil.containsFile(test6, test));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            deleteTempDir(test6);
            deleteTempDir(test);
        }
    }

    private void deleteTempDir(File location) {
        try {
            FileCommands.delete(location);
        } catch (IOException e) {
            fail("Could not delete temporary directory");
        }
    }

}
