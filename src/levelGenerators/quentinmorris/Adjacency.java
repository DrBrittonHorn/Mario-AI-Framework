package levelGenerators.quentinmorris;

public class Adjacency {
    private Tuple up, down, left, right;

    protected Adjacency(Tuple u, Tuple d, Tuple l, Tuple r){
        this.up = u;
        this.down = d;
        this.left = l;
        this.right = r;
    }

    public Tuple up(){ return up; }
    public Tuple down(){ return down; }
    public Tuple left(){ return left; }
    public Tuple right(){ return right; }

    public Tuple getFromDxdy(int dx, int dy){
        if(dx == -1 && dy == 0) return left;
        else if(dx == 1 && dy == 0) return right;
        else if(dy == -1 && dx == 0) return up;
        else if(dy == 1 && dx == 0) return down;
        else return null;
    }

    public void printAdj(){
        System.out.print("up: "); up.printTuple(); System.out.print("  ");
        System.out.print("right: "); right.printTuple(); System.out.print("  ");
        System.out.print("down: "); down.printTuple(); System.out.print("  ");
        System.out.print("left: "); left.printTuple(); System.out.println("");
    }
}
