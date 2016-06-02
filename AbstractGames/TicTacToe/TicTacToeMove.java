package AbstractGames.TicTacToe;

import AbstractGames.Move;
/**
 *
 */
public class TicTacToeMove extends Move {
  int x, y;
  int player;

  public TicTacToeMove () {
    x = y = player = 0;
    next = null;
  }

  public TicTacToeMove( int x, int y, int player) {
    this.x = x;
    this.y = y;
    this.player = player;
    this.next = null;
  }

  @SuppressWarnings("all")
  public String toString() {
    String result = new String();

    if (player == TicTacToeBoard.PLAYER_O)
      result = result.concat("O\n");
    else
      result = result.concat("X\n");

    result = result.concat( ": [" + Integer.toString(x) + ", " + Integer.toString(y) + "]");

    return result;
  }

  /**
   * Override equals
   * @param move
   * @return
   */
  public boolean equals(Move move) {
    TicTacToeMove m = (TicTacToeMove)move;
    if (m.x == this.x && m.y == this.y && m.player == this.player)
      return true;
    return false;
  }

}
