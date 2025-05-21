package levelGenerators.quentinmorris;

import java.util.Arrays;

//Stores the snapshot image taken from the input image

public class Snapshot {
    private int snapshotX;
    private int snapshotY;
    private char[][] snsh;

    protected Snapshot(int snapshotX, int snapshotY, char[][] snsh){
        this.snapshotX = snapshotX;
        this.snapshotY = snapshotY;
        this.snsh = snsh;
    }

    public void printSnapshot(){
        Utilities.PrintArray(snsh);
    }

    public int output(){
        return snsh[0][0];
    }

    @Override
    public boolean equals(Object s){
        if(s instanceof Snapshot) return Arrays.deepEquals(this.snsh, ((Snapshot)s).snsh);
        else return false;
    }

    @Override
    public int hashCode(){ return Arrays.deepHashCode(this.snsh); }
}
