package build.pluto.gitbuilder;

import org.junit.Test;
import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


public class InputTest {

    public void deleteResource(File file) {
        if (FileCommands.exists(file.toPath())) {
            try {
                FileCommands.delete(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testGetRepositoryName() {
        GitBuilder.Input in = new GitBuilder.Input("http://www.testurl.com/path/to/test.git", null);
        assertEquals("test", in.getRepositoryName());

        in = new GitBuilder.Input("ssh://www.testurl.com/test.git", null);
        assertEquals("test", in.getRepositoryName());

        in = new GitBuilder.Input("ftp://www.testurl.com/test", null);
        assertEquals("test", in.getRepositoryName());

        in = new GitBuilder.Input("https://www.testurl.com/", null);
        assertNull(in.getRepositoryName());

        in = new GitBuilder.Input("http://www.testurl.com", null);
        assertNull(in.getRepositoryName());

        in = new GitBuilder.Input("git://www.testurl.com", null);
        assertNull(in.getRepositoryName());
    }

    @Test
    public void testCheckLocalLocationContainsRepoName() {
        String localLocation = "test/local";
        File localLocationFile = new File(localLocation);
        GitBuilder.Input in = new GitBuilder.Input("https://www.testurl.com/repo", localLocationFile);
        try {
            FileCommands.createDir(localLocationFile.toPath());
            assertTrue(!in.checkLocalLocationContainsRepoDir());
            FileCommands.createDir(new File(localLocation + "/repo").toPath());
            assertTrue(in.checkLocalLocationContainsRepoDir());
        } catch (IOException e) {
            assertTrue(false);
        } finally {
            deleteResource(localLocationFile);
        }
    }

    @Test
    public void testIsLocalRepoValid() {
        File testDir = new File("test");
        GitBuilder.Input in = new GitBuilder.Input("git@github.com:andiderp/dummy.git", testDir);
        try {
            FileCommands.createDir(testDir.toPath());
            Exec.ExecutionResult result = Exec.run("git", "-C", testDir.getPath().toString(), "init", "dummy");
            assertTrue(in.isLocalRepoValid());
        } catch (IOException e) {
            assertTrue(false);
        } finally {
            deleteResource(testDir);
        }
    }

    @Test
    public void testCheckIfRemoteLocationIsRepository() {
        GitBuilder.Input in = new GitBuilder.Input("git@github.com:andiderp/dummy.git", null);
        assertTrue(in.checkIfRemoteLocationIsRepository());
    }

    @Test
    public void testCloneRepository() {
        File testDir = new File("test2");
        try {
            FileCommands.createDir(testDir.toPath());
            GitBuilder.Input in = new GitBuilder.Input("git@github.com:andiderp/dummy.git", testDir);
            in.cloneRepository();
            assertTrue(in.isLocalRepoValid());
        } catch (IOException e) {
            assertTrue(false);
        } finally {
            deleteResource(testDir);
        }
    }

    @Test
    public void testPullRepository() {
        File testDir = new File("test3");
        try {
            FileCommands.createDir(testDir.toPath());
            GitBuilder.Input in = new GitBuilder.Input("git@github.com:andiderp/dummy.git", testDir);

            in.cloneRepository();
            File file = new File("test3/dummy/README.md");
            String r = FileCommands.readFileAsString(file);
            assertEquals("This is a dummy repository for testing.\n", r);

            Exec.run("git", "-C", "test3/dummy", "reset", "--hard", "HEAD^");
            r = FileCommands.readFileAsString(file);
            assertEquals("This is a dummy repository for testing\n", r);

            in.pullRepository();
            r = FileCommands.readFileAsString(file);
            assertEquals("This is a dummy repository for testing.\n", r);
        } catch (IOException e) {
            assertTrue(false);
        } finally {
            deleteResource(testDir);
        }

    }
}