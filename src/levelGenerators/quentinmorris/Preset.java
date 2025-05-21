package levelGenerators.quentinmorris;

public class Preset {
    private Tuple loc;
    private char c;
    protected Preset(Tuple a, char b){
        this.loc = a;
        this.c = b;
    }

    public Tuple getLoc(){ return loc; }
    public char getC(){ return c; }
    //public void printTuple() {System.out.print("(" + a + "," + b + ")");}
}
