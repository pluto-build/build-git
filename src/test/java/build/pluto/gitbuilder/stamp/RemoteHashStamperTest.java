package build.pluto.gitbuilder.stamp;

import build.pluto.gitbuilder.Input;
import build.pluto.gitbuilder.exception.NotClonedException;
import build.pluto.gitbuilder.util.GitHandler;
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
        this.tested = new RemoteHashStamper(this.dummyPath, "master", null);
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
        GitHandler git = new GitHandler(input);
        try {
            git.cloneRepository();
        } catch (NotClonedException e) {
            fail("Could not clone directory");
        }
        RemoteHashStamper stamper = new RemoteHashStamper("jdkjdhjhfd", "master", null);
        ValueStamp s = (ValueStamp) stamper.stampOf(this.directory);
        assertEquals(s.val, "ddfa2acb09533f16792f6006316ce2744792d839");
    }

    private void deleteTempDirectory() {
        try {
            FileCommands.delete(directory.toPath());
        } catch (IOException e) {
            fail("Could not remove directory");
        }
    }
}
