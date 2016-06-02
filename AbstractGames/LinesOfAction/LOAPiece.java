package AbstractGames.LinesOfAction;

/**
 * Title:
 * Description:
 * Copyright: Copyright (c) 2004
 * Company:
 * @author Bert Peterson
 * @version 1.0
 */
public class LOAPiece {

    public int x;
    public int y;
    public int owner;
    public LOAPiece prev;
    public LOAPiece next;

    // Constructor
    LOAPiece(int X, int Y, int own) {
      x = X;
      y = Y;
      owner = own;
      next = null;
    }

    // Constructor
    LOAPiece(int X, int Y, int own, LOAPiece n, LOAPiece p) {
      x = X;
      y = Y;
      owner = own;
      next = n;
      prev = p;
      if (next != null)
        next.prev = this;
    }

}
