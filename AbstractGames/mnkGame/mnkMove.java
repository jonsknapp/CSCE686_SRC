package AbstractGames.mnkGame;

import AbstractGames.Move;

/**
 * Move class for the n,m,k-games
 */
public class mnkMove extends Move {
  int x, y;
  int player;

  public mnkMove () {
    x = y = player = 0;
    next = null;
  }

  public mnkMove( int x, int y, int player) {
    this.x = x;
    this.y = y;
    this.player = player;
    this.next = null;
  }

  @SuppressWarnings("all")
  public String toString() {
    String result = new String();

    if (player == mnkBoard.PLAYER_WHITE)
      result = result.concat("O");
    else
      result = result.concat("X");

    result = result.concat( ": [" + Integer.toString(x) + ", " + Integer.toString(y) + "]");

    return result;
  }

  /**
   * Override equals
   * @param move
   * @return
   */
  public boolean equals(Move move) {
    mnkMove m = (mnkMove)move;
    if (m.x == this.x && m.y == this.y && m.player == this.player)
      return true;
    return false;
  }
}
