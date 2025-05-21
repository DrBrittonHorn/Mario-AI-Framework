package levelGenerators.quentinmorris;

public class Tuple {
    private int a;
    private int b;
    protected Tuple(int a, int b){
        this.a = a;
        this.b = b;
    }

    public int _1(){ return a; }
    public int _2(){ return b; }
    public void printTuple() {System.out.print("(" + a + "," + b + ")");}
}
