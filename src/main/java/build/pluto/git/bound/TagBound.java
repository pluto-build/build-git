package build.pluto.git.bound;

public class TagBound implements UpdateBound {

    private final String tag;

    public TagBound(String tag) {
        this.tag = tag;
    }
    
    public String getBound() {
        return this.tag;
    }

    public String getBoundHash() {
        return this.tag;
    }

    public boolean reachedBound(String currentHash) {
        return currentHash.equals(this.tag); 
    }
}
