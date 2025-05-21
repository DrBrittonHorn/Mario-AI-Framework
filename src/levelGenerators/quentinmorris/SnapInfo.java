package levelGenerators.quentinmorris;

import java.util.ArrayList;

//A snapshot and all of its info

public class SnapInfo {
    private int freq;
    private ArrayList<Adjacency> adjs;

    protected SnapInfo(int f, ArrayList<Adjacency> as){
        this.freq = f;
        this.adjs = as;
    }

    public void increment(int i){ freq = freq+i; }
    public void addAdj(Adjacency a){ adjs.add(a); }

    public int getFreqency(){ return this.freq; }
    public ArrayList<Adjacency> getAdjs(){ return adjs; }

    public void printAdjs(){
        for(Adjacency a : adjs){ a.printAdj(); }
    }
}
