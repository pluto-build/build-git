package build.pluto.gitbuilder;

import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
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

    @Test
    public void checkIsRemoteAccessible() {
        Input in = this.createInput("test6", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertTrue(b.isRemoteAccessible(in));
    }

    @Test
    public void checkIsRemoteAccessibleNot() {
        Input in = this.createInput("test7", "https://github.com/andidep/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        assertFalse(b.isRemoteAccessible(in));
    }

    @Test
    public void checkClone() {
        Input in = this.createInput("test8", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        this.deleteTempDir(in.local);
        assertFalse(FileCommands.exists(in.local));
        try {
            b.clone(in);
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
        assertTrue(FileCommands.exists(in.local));
        boolean fileExists = false;
        for (Path p : FileCommands.listFilesRecursive(in.local.toPath())) {
            if (p.getFileName().toString().equals("README.md")) {
                fileExists = true;
            }
        }
        assertTrue(fileExists);
        deleteTempDir(in.local);
    }

    @Test
    public void checkIsLocalRemoteSet() {
        Input in = this.createInput("test9", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            b.clone(in);
            assertTrue(b.isRemoteSet(in));
        } catch (NotClonedException e) {
            fail("could not clone repository");
        } finally {
            deleteTempDir(in.local);
        }
    }

    @Test
    public void checkIsLocalRemoteNotSet() {
        Input in = this.createInput("test10", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        try {
            b.clone(in);
            Repository repo = Git.open(in.local).getRepository();
            StoredConfig config = repo.getConfig();
            config.unsetSection("remote", "origin");
            config.save();
            assertFalse(b.isRemoteSet(in));
        } catch (IOException e) {
            fail("Could not open repository");
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        } finally {
            deleteTempDir(in.local);
        }
    }

    @Test
    public void checkPull() {
        Input in = this.createInput("test11", "https://github.com/andiderp/dummy.git", "master");
        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
        this.deleteTempDir(in.local);
        try {
            b.clone(in);
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
        try {
            String content = FileCommands.readFileAsString(new File(in.local, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            Git.open(in.local).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
            content = FileCommands.readFileAsString(new File(in.local, "README.md"));
            assertEquals(content, "This is a dummy repository for testing\n");
            try {
                b.pull(in);
            } catch (NotPulledException e) {
                fail("Could not pull repository");
            }
            content = FileCommands.readFileAsString(new File(in.local, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } finally {
            deleteTempDir(in.local);
        }

    }

//    @Test
//    public void checkPullWithLocalCommit() {
//        Input in = this.createInput("test4", "https://github.com/andiderp/dummy.git", "master");
//        GitRepositoryBuilder b = new GitRepositoryBuilder(in);
//        this.deleteTempDir(in.local);
//        b.clone(in);
//        try {
//            String content = FileCommands.readFileAsString(new File(in.local, "README.md"));
//            assertEquals(content, "This is a dummy repository for testing.\n");
//            Git.open(in.local).reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD^").call();
//            content = FileCommands.readFileAsString(new File(in.local, "README.md"));
//            assertEquals(content, "This is a dummy repository for testing\n");
//            FileCommands.writeToFile(new File(in.local, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
//            Git.open(in.local).add().addFilepattern("README.md").call();
//            Git.open(in.local).commit().setMessage("local changes").call();
//            b.pull(in);
//            content = FileCommands.readFileAsString(new File(in.local, "README.md"));
//            System.out.println(content);
//            assertEquals(content, "This is a dummy repository for testing.\n\nLocal Change.\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (CheckoutConflictException e) {
//            e.printStackTrace();
//        } catch (GitAPIException e) {
//            e.printStackTrace();
//        } finally {
//            deleteTempDir(in.local);
//        }
//    }

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
