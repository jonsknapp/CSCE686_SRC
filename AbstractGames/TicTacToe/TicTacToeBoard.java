package AbstractGames.TicTacToe;

import AbstractGames.*;

/**
 *
 */
public class TicTacToeBoard extends Board {

  static final int PLAYER_X = 1;
  static final int PLAYER_O = -1;
  static final int GAME_DRAW = -2;
  static final int BLANK = 0;
  static final int BOARD_X = 3;
  static final int BOARD_Y = 3;

  private int[][] board = new int[BOARD_X][BOARD_Y];

  public int to_move;


  public TicTacToeBoard() {
    for (int x = 0; x < BOARD_X; x++ ){
      for (int y = 0; y < BOARD_Y; y++ ) {
        board[x][y] = BLANK;
      }
    }
    to_move = PLAYER_X;
  }

  public void loadBoard(String boardString) {
    char [] boardChars = boardString.toCharArray();
    for (int x = 0; x < BOARD_X; x++ ){
      for (int y = 0; y < BOARD_Y; y++ ) {
        if (boardChars[BOARD_X*x+y] == 'X')
          board[x][y] = PLAYER_X;
        else if (boardChars[BOARD_X*x+y] == 'O')
          board[x][y] = PLAYER_O;
        else
          board[x][y] = BLANK;
      }
    }
    if (boardChars[BOARD_X*BOARD_Y] == 'O')
      to_move = PLAYER_O;
    else
      to_move = PLAYER_X;
    System.out.println(this.toString());
  }

  public int getCurrentPlayer() {
    return to_move;
  }

  public int[] getPlayerList() {
    return new int[]{PLAYER_O, PLAYER_X};
  }

  public Move generateMoves() {
    TicTacToeMove result = null;

    for (int x = 0; x < BOARD_X; x++) {
      for (int y = 0; y < BOARD_Y; y++) {
        if ( board[x][y] == BLANK) {
          TicTacToeMove move = new TicTacToeMove(x, y, to_move);
          move.next = result;
          result = move;
        }
      }
    }

    return result;
  }

  public boolean makeMove(Move m) {
    TicTacToeMove ttm = (TicTacToeMove)m;
    if (board[ttm.x][ttm.y] == BLANK) {
      board[ttm.x][ttm.y] = to_move;
      to_move = -to_move;
      return true;
    }
    return false;
  }

  public boolean reverseMove(Move m) {
    TicTacToeMove ttm = (TicTacToeMove)m;
    if (board[ttm.x][ttm.y] == ttm.player) {
      board[ttm.x][ttm.y] = BLANK;
      to_move = -to_move;
      return true;
    }
    return false;
  }

  public double heuristicEvaluation() {
    return 0.0;
  }

  public Move moveOrdering(Move move_list, int depth) {
    // Assign a value to each move and
    // return Util.QuickSort(move_list)
    return move_list;
  }

  public void moveOrderingData(Move best_move, int depth, boolean pruned) {
  }

  public int endGame() {
    int count;
    int draw = 0;

    // Column Check
    for (int x = 0; x < BOARD_X; x++) {
      count = 0;
      for (int y = 0; y < BOARD_Y; y++) {
        count += board[x][y];
        draw  += Math.abs(board[x][y]);
      }
      if (count == PLAYER_X * BOARD_X)
        return PLAYER_X;
      if (count == PLAYER_O * BOARD_X)
        return PLAYER_O;
    }
    // Row Check
    for (int y = 0; y < BOARD_Y; y++) {
      count = 0;
      for (int x = 0; x < BOARD_X; x++) {
        count += board[x][y];
      }
      if (count == PLAYER_X * BOARD_Y)
        return PLAYER_X;
      if (count == PLAYER_O * BOARD_Y)
        return PLAYER_O;
    }
    //Diagonal check (note: the others will work if the board is not square, this one assumes it is)
    count = 0;
    for (int x = 0; x < BOARD_X; x++) {
      count += board[x][x];
    }
    if (count == PLAYER_X * BOARD_X)
      return PLAYER_X;
    if (count == PLAYER_O * BOARD_X)
      return PLAYER_O;

    count = 0;
    for (int x = 0; x < BOARD_X; x++) {
      count += board[x][BOARD_X-x-1];
    }
    if (count == PLAYER_X * BOARD_X)
      return PLAYER_X;
    if (count == PLAYER_O * BOARD_X)
      return PLAYER_O;

    if (draw == BOARD_X*BOARD_Y)
      return Board.GAME_DRAW;

    return BLANK;
  }


  @SuppressWarnings("all")
  public String toString() {
    String result = new String();

    for (int x = 0; x < BOARD_X; x++ ){
      for (int y = 0; y < BOARD_Y; y++ ) {
        if (board[x][y] == PLAYER_O)
          result = result.concat("O ");
        else if (board[x][y] == PLAYER_X)
          result = result.concat("X ");
        else
          result = result.concat("  ");
      }
      result = result.concat("\n");
    }

    return result;
  }

  public TicTacToeMove newMove() {
    return new TicTacToeMove(0,0,0);
  }
  
  public void printBoardRep()
  {
  }
}
