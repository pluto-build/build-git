package build.pluto.git.stamp;

import build.pluto.git.Input;
import build.pluto.git.bound.BranchBound;
import build.pluto.git.exception.NotClonedException;
import build.pluto.git.util.GitHandler;
import build.pluto.stamp.ValueStamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class RemoteHashStamperTest {

    private RemoteHashStamper tested;
    private File directory;
    private String dummyPath;

    @Before
    public void init() {
        File remoteDirectory = new File("src/test/resources/dummy");
        this.directory = new File("test");
        try {
            FileCommands.createDir(directory.toPath());
        } catch (IOException e) {
            fail("Could not create directory");
        }
        this.dummyPath = "file://" + remoteDirectory.getAbsolutePath();
        BranchBound bound = new BranchBound(this.dummyPath, "master");
        this.tested = new RemoteHashStamper(this.dummyPath, bound);
    }

    @After
    public void destroy() {
        this.deleteTempDirectory();
    }

    @Test
    public void testDirectoryIsEmpty() {
        this.deleteTempDirectory();
        ValueStamp s = (ValueStamp) this.tested.stampOf(directory);
        assertNull(s.val);
    }

    @Test
    public void testRemoteIsAccessible() {
        ValueStamp s = (ValueStamp) this.tested.stampOf(directory);
        assertEquals(s.val, "ddfa2acb09533f16792f6006316ce2744792d839");
    }

    @Test
    public void testRemoteIsNotAccessible() {
        Input.Builder inputBuilder = new Input.Builder(directory, this.dummyPath, null);
        Input input = inputBuilder.build();
        try {
            GitHandler.cloneRepository(input);
        } catch (NotClonedException e) {
            fail("Could not clone directory");
        }
        BranchBound bound = new BranchBound(this.dummyPath, "master");
        RemoteHashStamper stamper = new RemoteHashStamper("jdkjdhjhfd", bound);
        ValueStamp s = (ValueStamp) stamper.stampOf(this.directory);
        assertEquals("ddfa2acb09533f16792f6006316ce2744792d839", s.val);
    }

    private void deleteTempDirectory() {
        try {
            FileCommands.delete(directory.toPath());
        } catch (IOException e) {
            fail("Could not remove directory");
        }
    }
}
