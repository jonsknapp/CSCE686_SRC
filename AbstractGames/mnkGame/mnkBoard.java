package AbstractGames.mnkGame;

import AbstractGames.Board;
import AbstractGames.Move;

/**
 * This is the m,n,k-game board class. This is a generalization of m x n games in which the
 * players are trying to get k pieces in a row. For example, 3, 3, 3 is Tic-Tac-Toe.
 * 15, 15, 5 would by Go-Moku. for Go-Moku, there are 15x15 intersections on the board. This code
 * would have to be augmented for the inclusion of the 'standard rule' that prevents overlines
 * (a row of 6 or more pieces being a win).
 *
 * Similar changes would be for Connect 4 (7, 7, 4) in which the move generation is limited to
 * dropping.
 *
 * The other extension is the m,n,k,p,q-game which adds placing p stones a move, and the first
 * player only starts with placing q stones. Connect6 is m,n,6,2,1.
 *
 * J. W. H. M. Uiterwijk and H. J van der Herik, The advantage of the initiative, Information Sciences 122 (1) (2000) 43-58.
 *
 * Stochastic, hidden information changes include:
 * phantom m,n,k-game in which the opponents piece locations are unknown (Whitehouse, et. al)
 * Dice in which a dice is rolled to determine which row for X or which column for O the player can place their piece.
 *  In Dice, n=m=k, testing was for 5, 7, 9, 11, 13, 15, 17. (Houk, 2004)
 *
 */
public class mnkBoard extends Board {
  static final int PLAYER_BLACK = 1;
  static final int PLAYER_WHITE = 0;
  static final int GAME_DRAW = -2;
  static final int EMPTY_SQUARE = -1;
  /**
   * n the horizontal board size
   */
  protected int boardX;
  /**
   * m the vertical board size
   */
  protected int boardY;
  /**
   * k is the number of pieces that need to be in a row, column, or diagonal to
   * be a considered a win.
   */
  protected int goalK;

  /**
   * The playing board.
   */
  private int[][] board;

  public int to_move;


  /**
   * Default constructor: creates a Tic-Tac-Toe instantiation.
   * Recommend not using.
   */
  public mnkBoard() {
    boardX = 3;
    boardY = 3;
    goalK = 3;
    board = new int[boardX][boardY];
    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        board[x][y] = EMPTY_SQUARE;
      }
    }
    to_move = PLAYER_BLACK;
  }

  public mnkBoard(int m, int n, int k) {
    boardX = m;
    boardY = n;
    goalK = k;
    board = new int[boardX][boardY];
    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
      board[x][y] = EMPTY_SQUARE;
    }
  }
  to_move = PLAYER_BLACK;
  }

  /**
   * This one will be a problem because of the variability of n, m and k
   *
   * @param boardString
   */
  public void loadBoard(String boardString) {
    char [] boardChars = boardString.toCharArray();
    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        if (boardChars[boardX*x+y] == 'X')
          board[x][y] = PLAYER_BLACK;
        else if (boardChars[boardX*x+y] == 'O')
          board[x][y] = PLAYER_WHITE;
        else
          board[x][y] = EMPTY_SQUARE;
      }
    }
    if (boardChars[boardX*boardY] == 'O')
      to_move = PLAYER_WHITE;
    else
      to_move = PLAYER_BLACK;
    System.out.println(this.toString());
  }

  public int getCurrentPlayer() {
    return to_move;
  }

  public int[] getPlayerList() {
    return new int[]{PLAYER_WHITE, PLAYER_BLACK};
  }

  public Move generateMoves() {
    mnkMove result = null;

    for (int x = 0; x < boardX; x++) {
      for (int y = 0; y < boardY; y++) {
        if (board[x][y] == EMPTY_SQUARE) {
          mnkMove move = new mnkMove(x, y, to_move);
          move.next = result;
          result = move;
        }
      }
    }

    return result;
  }

  /**
   * Update the game state based upon the move submitted.
   *
   * @param m a square to check
   * @return true if complete
   */
  public boolean makeMove(Move m) {
    mnkMove ttm = (mnkMove)m;
    if (board[ttm.x][ttm.y] == EMPTY_SQUARE) {
      board[ttm.x][ttm.y] = to_move;
      to_move = opponent(to_move);
      return true;
    }
    return false;
  }

  /**
   * Execute the removal of an action
   *
   * @param m Move the move to remove, should be the last move executed.
   */
  public boolean reverseMove(Move m) {
    mnkMove ttm = (mnkMove)m;
    if (board[ttm.x][ttm.y] == ttm.player) {
      board[ttm.x][ttm.y] = EMPTY_SQUARE;
      to_move = opponent(to_move);
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

  /**
   * endGame check. Check each row, column and diagonal for k in a row pieces. Then check
   * if all of the spaces are filled - in which case the game is a draw.
   * 
   * @return
   */
  public int endGame() {
    int winner = getWinnerInRows();
    if (winner != GAME_CONTINUE)
      return winner;
    winner = getWinnerInColumns();
    if (winner != GAME_CONTINUE)
      return winner;
    winner = getWinnerInDiagonals();
    if (winner != GAME_CONTINUE)
      return winner;

    // Now we need to check if there are empty positions, otherwise it is a draw
    for (int i = 0; i < boardX; ++i)
      for (int j = 0; j < boardY; ++j)
        if (board[i][j] == EMPTY_SQUARE) 
          return GAME_CONTINUE;

    return GAME_DRAW;
  }


  @SuppressWarnings("all")
  public String toString() {
    String result = new String();

    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        if (board[x][y] == PLAYER_WHITE)
          result = result.concat("O");
        else if (board[x][y] == PLAYER_BLACK)
          result = result.concat("X");
        else
          result = result.concat(" ");
      }
      result = result.concat("\n");
    }

    return result;
  }

  public mnkMove newMove() {
    return new mnkMove(0,0,0);
  }

  /**
   * Get the value stored in the board at location <x,y>. This can be
   * PLAYER_BLACK, PLAYER_WHITE, EMPTY_SQUARE
   *
   * @param x column
   * @param y row
   * @return {PLAYER_BLACK, PLAYER_WHITE, EMPTY_SQUARE}
   */
  public int getPlayerAtLocation(int x, int y) {
    return board[x][y];
  }

  public void setPlayerAtLocation(int x, int y, int side) {
    board[x][y] = side;
  }

  public int getBoardX() {return boardX;}

  public int getBoardY() {return boardY;}

  /**
   * Outputs the opposite color of the player color sent in.
   *
   * @param player Integer Player's color.
   * @return Integer [PLAYER_WHITE,PLAYER_BLACK]
   */
  final int opponent(int player) {
    if (player == PLAYER_WHITE)
      return PLAYER_BLACK;
    if (player == PLAYER_BLACK)
      return PLAYER_WHITE;
    throw new Error("internal error: bad player " + player);
  }

  /**
   * Checks each of the board rows for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInRows() {
    // Check rows and see if there are k pieces of the same color
    for (int row = 0; row < boardY; ++row) {
      int count = 1;
      // We will compare current element with the previous
      for (int column = 1; column < boardX; ++column) {
        if (board[row][column] != EMPTY_SQUARE &&
            board[row][column] == board[row][column-1])
          ++count;
        else
          count = 1;

        // Check if there are k in a row.
        if (count >= goalK) {
          // Return color of the winner
          return board[row][column];
        }
      }
    }
    // Otherwise return GAME_CONTINUE, which means nobody win in rows.
    return GAME_CONTINUE;
  }

  /**
   * Checks each of the board columns for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInColumns() {
    // Check rows and see if there are k pieces of the same color
    for (int column = 0; column < boardX; ++column) {
      int count = 1;
      // We will compare current element with the previous
      for (int row = 1; row < boardY; ++row) {
        if (board[row][column] != EMPTY_SQUARE &&
            board[row][column] == board[row-1][column])
          ++count;
        else
          count = 1;

        // Check if there are k in a row.
        if (count >= goalK) {
          // Return color of the winner
          return board[row][column];
        }
      }
    }
    // Otherwise return GAME_CONTINUE, which means nobody win in columns.
    return GAME_CONTINUE;
  }

  /**
   * Checks each of the board diagonals for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInDiagonals() {
    int count;

    // Check the lower-left to upper-right diagonals -> /
    for(int y = boardY - goalK; y >= 0; y--) {
      for (int x = 0; x < boardX; x++) {
        count = 1;
        for (int diag = 0; ((x+diag+1) < boardX && (y+diag+1) < boardY); diag++) {
          if ((board[x+diag][y+diag] != EMPTY_SQUARE) &&
              (board[x+diag][y+diag] == board[x+diag+1][y+diag+1]))
            count++;
          else
            count = 1;
          if ( count >= goalK) {
            return board[x+diag][y+diag];
          }
        }
      }
    }

    // Check the upper-left to lower-right diagonals -> \
    for (int x = boardX - goalK; x >= 0; x--) {
      for (int y = boardY -1 ; y > 0; y--) {
        count = 1;
        for (int diag = 0; ((y-diag) > 0 && (x+diag+1) < boardX); diag++) {
          if ((board[x+diag][y-diag] != EMPTY_SQUARE) &&
              (board[x+diag][y-diag] == board[x+diag+1][y-diag-1]))
            count++;
          else
            count = 1;
          if (count >= goalK)
            return board[x+diag][y-diag];
        }
      }
    }

    // Otherwise return GAME_CONTINUE.
    return GAME_CONTINUE;
  }
  
  public void printBoardRep()
  {
  }

}
