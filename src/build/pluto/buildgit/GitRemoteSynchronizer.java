package build.pluto.buildgit;

import java.io.File;
import java.util.List;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildgit.dependency.GitRemoteRequirement;
import build.pluto.buildgit.util.FileUtil;
import build.pluto.buildgit.util.GitHandler;
import build.pluto.output.None;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.FileIgnoreStamper;
import build.pluto.stamp.Stamper;

public class GitRemoteSynchronizer extends Builder<GitInput, None> {

  public static BuilderFactory<GitInput, None, GitRemoteSynchronizer> factory = BuilderFactoryFactory.of(GitRemoteSynchronizer.class, GitInput.class);

  public GitRemoteSynchronizer(GitInput input) {
    super(input);
  }

  @Override
  protected String description(GitInput input) {
    return "Git sync " + input.directory + " with remote " + input.url + " at " + input.bound.getBound();
  }

  @Override
  public File persistentPath(GitInput input) {
    return new File(input.directory, ".git/git.dep");
  }

  @Override
  protected None build(GitInput input) throws Throwable {
    boolean isRepo = GitHandler.isRepo((input.directory));
    boolean isDirEmpty = FileUtil.isDirectoryEmpty(input.directory);
    if (!isDirEmpty && !isRepo)
      throw new IllegalArgumentException(input.directory + " contains other data");

    if (!isDirEmpty && isRepo) {
      GitHandler.checkout(input.directory, input.bound.getBound());
      if (GitHandler.isUrlAccessible(input.url))
        GitHandler.pull(input);
    } else {
      try {
        GitHandler.cloneRepository(input);
        GitHandler.resetRepoToCommit(input.directory, input.bound.getBoundHash());
      } finally {
        File tsPersistentPath = new File(input.directory, ".git/git.dep.time");
        GitRemoteRequirement gitRequirement = new GitRemoteRequirement(
            input.directory,
            input.bound,
            input.url,
            input.consistencyCheckInterval,
            tsPersistentPath);
        requireOther(gitRequirement);
      }
    }

    // provide files
    Stamper stamper = input.allowLocalChanges ? FileIgnoreStamper.instance : FileHashStamper.instance;
    List<File> outputFiles = GitHandler.getNotIgnoredFilesOfRepo(input.directory);
    for (File f : outputFiles)
      this.provide(f, stamper);

    return None.val;
  }
}
