package build.pluto.gitbuilder;

import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import org.eclipse.jgit.api.Git;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class GitRepositoryBuilderTest {

    @Test
    public void checkLocalLocationExists() {
        Input in = this.createInput("test", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        File local = new File("test");
        try {
            FileCommands.createDir(local.toPath());
            assertTrue(b.localLocationExists(in));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.deleteTempDir(local);
        }
    }

    @Test
    public void checkLocalLocationExistsNot() {
        Input in = this.createInput("test2", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertFalse(b.localLocationExists(in));
    }

    @Test
    public void checkIsLocalLocationEmpty() {
        Input in = this.createInput("test7", null, null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.localLocation.toPath());
            assertTrue(b.localLocationIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create directory");
        } finally {
            deleteTempDir(in.localLocation);
        }
    }

    @Test
    public void checkIsLocalLocationNotEmpty() {
        Input in = this.createInput("test8", null, null);
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            FileCommands.createDir(in.localLocation.toPath());
            File file = new File(in.localLocation, "test.txt");
            FileCommands.createFile(file);
            assertFalse(b.localLocationIsEmpty(in));
        } catch (IOException e) {
            fail("Could not create directory");
        } finally {
            deleteTempDir(in.localLocation);
        }
    }

    @Test
    public void checkIsRemoteLocationAccessible() {
        Input in = this.createInput("test3", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertTrue(b.isRemoteLocationAccessible(in));
    }

    @Test
    public void checkIsRemoteLocationAccessibleNot() {
        Input in = this.createInput("test3", "https://github.com/andidep/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertFalse(b.isRemoteLocationAccessible(in));
    }

    @Test
    public void checkCloneRepository() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        this.deleteTempDir(in.localLocation);
        assertFalse(FileCommands.exists(in.localLocation));
        b.cloneRepository(in);
        assertTrue(FileCommands.exists(in.localLocation));
        boolean fileExists = false;
        for (Path p : FileCommands.listFilesRecursive(in.localLocation.toPath())) {
            if (p.getFileName().toString().equals("README.md")) {
                fileExists = true;
            }
        }
        assertTrue(fileExists);
        deleteTempDir(in.localLocation);
    }

    @Test
    public void checkIsLocalLocationRemoteSet() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        b.cloneRepository(in);
        assertTrue(b.isRemoteSet(in));
        deleteTempDir(in.localLocation);
    }

    @Test
    public void checkIsLocalLocationRemoteNotSet() {
        Input in = this.createInput("test6", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            b.cloneRepository(in);
            Repository repo = Git.open(in.localLocation).getRepository();
            StoredConfig config = repo.getConfig();
            config.clear();
//            assertFalse(b.isRemoteSet(in));
        } catch (IOException e) {
            fail("Could not create directory");
        } finally {
            deleteTempDir(in.localLocation);
        }
    }

    @Test
    public void checkPull() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        this.deleteTempDir(in.localLocation);
        b.cloneRepository(in);
        try {
            String content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            Git.open(in.localLocation).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
            content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            assertEquals(content, "This is a dummy repository for testing\n");
            b.pullRepository(in);
            content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

//    @Test
    public void checkPullWithLocalCommit() {
        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        this.deleteTempDir(in.localLocation);
        b.cloneRepository(in);
        try {
            String content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            Git.open(in.localLocation).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
            content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            assertEquals(content, "This is a dummy repository for testing\n");
            FileCommands.writeToFile(new File(in.localLocation, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
            Git.open(in.localLocation).add().addFilepattern("README.md").call();
            Git.open(in.localLocation).commit().setMessage("local changes").call();
            b.pullRepository(in);
            content = FileCommands.readFileAsString(new File(in.localLocation, "README.md"));
            System.out.println(content);
            assertEquals(content, "This is a dummy repository for testing.\n\nLocal Change.\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
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
