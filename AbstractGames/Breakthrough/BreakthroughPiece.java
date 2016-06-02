package AbstractGames.Breakthrough;
/**
 *
 */

public class BreakthroughPiece
{
  public int x;
  public int y;
  public int owner;
  public BreakthroughPiece prev;
  public BreakthroughPiece next;


  // Constructor
  BreakthroughPiece(int X, int Y, int own)
  {
    x = X;
    y = Y;
    owner = own;
    next = null;
  }


  // Constructor
  BreakthroughPiece(int X, int Y, int own, BreakthroughPiece n, BreakthroughPiece p)
  {
    x = X;
    y = Y;
    owner = own;
    next = n;
    prev = p;
    if (next != null)
      next.prev = this;
  }


  public String toString()
  {
    return owner + " (" + x + "," + y + ")";
  }

}
