package build.pluto.gitbuilder;

import build.pluto.builder.Builder;
import build.pluto.output.None;
import org.sugarj.common.Exec;

import java.io.File;
import java.io.Serializable;

public class GitBuilder extends Builder<GitBuilder.Input, None> {

    public static class Input implements Serializable {
        private static final long serialVersionUID = -1L;

        public String remoteLocation;

        public File localLocation;

        public Input(String remoteLocation, File localLocation) {
            this.remoteLocation = remoteLocation;
            this.localLocation = localLocation;
        }

        private String getGitPrefix() {
            return "git -C" + localLocation.getPath().toString();
        }
        public boolean checkIfRemoteLocationIsRepository() {
            try {
                Exec.ExecutionResult result = Exec.run("git", "ls-remote", remoteLocation);
            } catch (Exec.ExecutionError e) {
                return false;
            }
            return true;
        }

        public boolean checkLocalLocationContainsRepoDir() {
            for (File file : localLocation.listFiles()) {
                if (file.isDirectory() && file.getName().equals(getRepositoryName())) {
                    return true;
                }
            }
            return false;
        }

        public String getRepositoryName() {
            int beginningOfRepoName = remoteLocation.lastIndexOf("/") + 1;
            if (beginningOfRepoName != -1 &&
                remoteLocation.substring(beginningOfRepoName - 3, beginningOfRepoName).equals("://")) {
                //TODO:think why this url would not be valid
                return null; //maybe throw an exception
            }
            String repoName = remoteLocation.substring(beginningOfRepoName).replace(".git", "");
            if (repoName.equals("")) {
                return null;
            }
            return repoName;
        }

        public boolean isLocalRepoValid() {
            try {
                Exec.ExecutionResult result = Exec.run("git", "-C", localLocation.getPath().toString()+"/"+getRepositoryName(), "fsck", "--strict");
            } catch (Exec.ExecutionError e) {
                return false;
            }
            return true;
        }
        public void cloneRepository() {
            Exec.run("git", "-C", localLocation.getPath().toString(), "clone", remoteLocation.toString());
        }

        public void pullRepository() {
            Exec.run("git", "-C", localLocation.getPath().toString()+"/"+getRepositoryName(), "pull");
        }
    }
    public GitBuilder(Input input) {
        super(input);
    }

    @Override
    protected String description(Input in) {
        return "Keep the Git repository up-to-date for " + in.remoteLocation;
    }

    @Override
    protected File persistentPath(Input in) {
        return null;
    }

    @Override
    protected None build(Input in) throws Throwable {
        if (in.checkIfRemoteLocationIsRepository()) {
            if (!in.checkLocalLocationContainsRepoDir()) {
                in.cloneRepository();
            }
        }
        return None.val;
    }
}