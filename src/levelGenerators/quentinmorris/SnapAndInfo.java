package levelGenerators.quentinmorris;

//Combination of a snapshot and all of its info

public class SnapAndInfo {
    private Snapshot sn;
    private SnapInfo info;
    protected SnapAndInfo(Snapshot s, SnapInfo i){
        this.sn = s;
        this.info = i;
    }

    public Snapshot getSn(){ return sn; }
    public SnapInfo getInfo(){ return info; }
}
