package levelGenerators.quentinmorris;

//coupling of a snapshot and its adjacencies

public class SnapAdjs {
    private Snapshot sn;
    private Adjacency adj;

    protected SnapAdjs(Snapshot s, Adjacency as){
        this.sn = s; this.adj = as;
    }

    public Snapshot getSnapshot(){ return sn; }
    public Adjacency getAdj(){ return adj; }
}
