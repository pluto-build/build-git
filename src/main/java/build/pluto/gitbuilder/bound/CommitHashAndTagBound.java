package build.pluto.gitbuilder.bound;

public class CommitHashAndTagBound implements UpdateBound {

    private final String commitHashOrTag;

    public CommitHashAndTagBound(String commitHashOrTag) {
        this.commitHashOrTag = commitHashOrTag;
    }

    public String getBoundHash() {
        return this.commitHashOrTag;
    }

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(commitHashOrTag); 
    }
}
