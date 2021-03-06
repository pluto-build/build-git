package build.pluto.buildgit.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.sugarj.common.FileCommands;

public class FileUtilTest {

    private final File testDir = new File("fileutiltest");
    @After
    public void destroy() {
        try {
            FileCommands.delete(this.testDir);
        } catch (IOException e) {
            fail("Could not delete temporary directory");
        }
    }

    @Test
    public void checkDirectoryIsEmpty() {
        try {
            FileCommands.createDir(this.testDir.toPath());
            assertTrue(FileUtil.isDirectoryEmpty(this.testDir));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        }
    }

    @Test
    public void checkDirectoryContainsDotFile() {
        try {
            FileCommands.createDir(this.testDir.toPath());
            File dotFile = new File(this.testDir, ".dot");
            FileCommands.createFile(dotFile);
            assertFalse(FileUtil.isDirectoryEmpty(this.testDir));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        }
    }

    @Test
    public void checkDirectoryIsNotEmpty() {
        try {
            FileCommands.createDir(this.testDir.toPath());
            File file = new File(this.testDir, "test.txt");
            FileCommands.createFile(file);
            assertFalse(FileUtil.isDirectoryEmpty(this.testDir));
        } catch (IOException e) {
            fail("Could not create temporary directory");
        }
    }

    @Test
    public void checkContainsFile() {
        File testSubDir = new File(this.testDir, ".test");
        File testFile = new File(this.testDir, "test.txt");
        File testFile2 = new File(testSubDir, "test.txt");
        try {
            FileCommands.createDir(this.testDir.toPath());
            FileCommands.createDir(testSubDir.toPath());
            FileCommands.createFile(testFile);
            FileCommands.createFile(testFile2);
            assertTrue(FileUtil.containsFile(this.testDir, testFile));
            assertTrue(FileUtil.containsFile(this.testDir, testFile2));
            assertFalse(FileUtil.containsFile(testSubDir, testFile));
            assertFalse(FileUtil.containsFile(testSubDir, this.testDir));
            assertTrue(FileUtil.containsFile(testSubDir, testFile2));
            assertTrue(FileUtil.containsFile(this.testDir, testSubDir));
        } catch (IOException e) {
            fail("Could not create directories");
        }
    }
}
