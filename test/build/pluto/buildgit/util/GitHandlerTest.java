package build.pluto.buildgit.util;

import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.bound.BranchBound;
import build.pluto.buildgit.bound.CommitHashBound;
import build.pluto.buildgit.bound.TagBound;
import build.pluto.buildgit.bound.UpdateBound;
import build.pluto.buildgit.exception.NotCheckedOutException;
import build.pluto.buildgit.exception.NotClonedException;
import build.pluto.buildgit.exception.NotPulledException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GitHandlerTest {

    private GitInput in;
    private String dummyPath;
    private static final File testRemote = new File("src/test/resources/dummy");
    private final String featureHeadHash = "c55e35f7b4d3ff14cb8a99268e6ae0439e6c0d6f";
    private final String masterHeadHash = "ddfa2acb09533f16792f6006316ce2744792d839";
    private final String tagHash = "3d8913c40c2387488172368a5cf9cf5eb64fed88";

    @BeforeClass
    public static void cloneRepo() {
        if(testRemote.exists()) {
            return;
        }
        GitInput input = new GitInput.Builder(
                testRemote,
                "https://github.com/andiderp/dummy")
            .addBranchToClone("feature")
            .addBranchToClone("master2")
            .build();
        try {
            GitHandler.cloneRepository(input);
        } catch (Exception e) {
            fail("Could not setup class");
        }
    }

    // @AfterClass
    // public static void delteRepo() {
    //     try {
    //         FileCommands.delete(testRemote);
    //     } catch (IOException e) {
    //         fail("Could not delete test repository");
    //     }
    // }

    @Before
    public void init() {
        File dummyLocation = new File("src/test/resources/dummy");
        this.dummyPath = "file://" + dummyLocation.getAbsolutePath();
        this.in = this.createInput("testLocal", this.dummyPath);
    }

    @After
    public void destroy() {
        try {
            FileCommands.delete(in.directory);
        } catch (IOException e) {
            fail("Could not delete temporary directory");
        }
    }

    @Test
    public void checkIsUrlAccessible() {
        assertTrue(GitHandler.isUrlAccessible(in.url));
    }

    @Test
    public void checkNotIsUrlAccessible() {
        assertFalse(GitHandler.isUrlAccessible("deadlink"));
    }

    @Test
    public void checkClone() {
        clone(in);
        boolean fileExists = false;
        for (Path p : FileCommands.listFilesRecursive(in.directory.toPath())) {
            if (p.getFileName().toString().equals("README.md")) {
                fileExists = true;
            }
        }
        assertTrue(fileExists);
    }

    @Test
    public void checkIsUrlSet() {
        clone(in);
        assertTrue(GitHandler.isUrlSet(in.directory, in.url));
    }

    @Test
    public void checkIsUrlNotSet() {
        try {
            clone(in);
            Repository repo = Git.open(in.directory).getRepository();
            StoredConfig config = repo.getConfig();
            config.unsetSection("remote", "origin");
            config.save();
            assertFalse(GitHandler.isUrlSet(in.directory, in.url));
        } catch (IOException e) {
            fail("Could not open repository");
        }
    }

    @Test
    public void checkPull() throws InvalidRefNameException {
        try {
            clone(in);
            String content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            GitHandler.pull(in);
            content = FileCommands.readFileAsString(new File(in.directory, "README.md"));
            assertEquals(content, "This is a dummy repository for testing.\n");
        } catch (IOException e) {
            fail("Could not read file");
        } catch (NotPulledException e) {
            e.printStackTrace();
            fail("Could not pull repository");
        }
    }

    @Test(expected = NotPulledException.class)
    public void checkPullWithLocalCommit() throws NotPulledException {
        clone(in);
        try {
            GitHandler.resetRepoToCommit(in.directory, "HEAD^");
            FileCommands.writeToFile(new File(in.directory, "README.md"), "This is a dummy repository for testing.\nLocal Change.");
            GitHandler.add(in.directory, "README.md");
            GitHandler.commit(in.directory, "local changes");
        } catch (IOException e) {
            fail("Could not open the repository");
        } catch (GitAPIException e) {
            fail("Could not commit local change");
        }
        GitHandler.pull(in);
    }

    @Test
    public void testCheckout() {
        clone(in);
        Exception ex = null;
        try {
            GitHandler.checkout(in.directory, "feature");
        } catch (NotCheckedOutException e ) {
            ex = e;
        }
        assertNull(ex);
    }

    @Test(expected = NotCheckedOutException.class)
    public void testCheckoutFailed() throws NotCheckedOutException {
        clone(in);
        GitHandler.checkout(in.directory, "master2");
    }

    @Test
    public void checkGetHashOfRemoteHEADMaster() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "master");
        assertEquals(masterHeadHash, s);
    }

    @Test
    public void checkGetHashOfRemoteHEADFeature() {
        String s = GitHandler.getHashOfRemoteHEAD(this.dummyPath, "feature");
        assertEquals(featureHeadHash, s);
    }

    @Test
    public void checkGetHashOfCommitHashBound(){
        UpdateBound bound = new CommitHashBound(masterHeadHash);
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(masterHeadHash, s);
    }

    @Test
    public void checkGetHashOfTagBound(){
        UpdateBound bound = new TagBound(dummyPath, "v0.1");
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(tagHash, s);
    }

    @Test
    public void checkGetHashOfBranchBound(){
        UpdateBound bound = new BranchBound(dummyPath, "feature");
        String s = GitHandler.getHashOfBound(dummyPath, bound);
        assertEquals(featureHeadHash, s);
    }

    @Test
    public void checkGetNotIgnoredFilesOfRepoCorrectSize() {
        clone(in);
        List<File> foundFiles = GitHandler.getNotIgnoredFilesOfRepo(in.directory);
        assertEquals(foundFiles.size(), 2);
    }

    @Test
    public void checkListsUntrackedFiles() {
        clone(in);
        try{
            File dummyFile = new File(in.directory, "dummyFile.txt");
            FileCommands.createFile(dummyFile);
            List<File> foundFiles = GitHandler.getNotIgnoredFilesOfRepo(in.directory);
            assertEquals(foundFiles.size(), 3);
        } catch (IOException e) {
            fail("Could not create file");
        }
    }

    @Test
    public void checkListOnlyNotIgnoredFiles() {
        clone(in);
        try{
            File tempDir = new File(in.directory, "temp");
            File dummyFile = new File(tempDir, "ok.txt");
            FileCommands.createDir(tempDir.toPath());
            FileCommands.createFile(dummyFile);
            List<File> foundFiles = GitHandler.getNotIgnoredFilesOfRepo(in.directory);
            assertEquals(foundFiles.size(), 2);
        } catch (IOException e) {
            fail("Could not create file");
        }
    }

    private void clone(GitInput input) {
        try {
            GitHandler.cloneRepository(input);
        } catch (NotClonedException e) {
            fail("Could not clone repository");
        }
    }

    private GitInput createInput(String local, String remote) {
        File localFile = new File(local);
        GitInput.Builder inputBuilder = new GitInput.Builder(localFile, remote);
        inputBuilder.addBranchToClone("feature");
        return inputBuilder.build();
    }
}
