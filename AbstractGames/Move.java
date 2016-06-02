package AbstractGames;

import java.util.ArrayList;

/**
 * The Move class. Each Abstract game should have an extension of this class. It should store the action(s)
 * that a player can execute.
 */
public abstract class Move {

  public double value; /**< Store an estimate of the move for move ordering */
  public Move next;    /**< Pointer to the next move in the linked list */
  ArrayList<Double> h = null;

  /**
   * Base constructor.
   */
  public Move() {
    value = 0.0;
    next = null;
  }

  /**
   * Converts this Move into a String for printing
   * @return The move as a string
   */
  public String toString() {
    return null;
  }
  
  public void addH(double m)
  {
    if (h == null)
    {
      h = new ArrayList<Double>();
    }
    h.add(m);
  }
  
  public double getAverageH()
  {
    double result = 0;
    if (h != null)
    {
      double total = 0;
      for (double m: h)
      {
        total = total + m;
      }
      result = total / (h.size() * 1.0);
    }
    return result;
  }

  /**
   * Compare two moves
   * @param Move object to compare with this one
   * @return true if the moves are the same false if not
   */
  public abstract boolean equals(Move move);
}
