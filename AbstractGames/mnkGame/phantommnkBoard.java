package AbstractGames.mnkGame;

import AbstractGames.*;
import AbstractGames.Util;

/**
 *
 */
public class phantommnkBoard extends StochasticBoard {

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
   * A board is maintained for each player's perspective. The stochastic searches
   * then use this board so that the search only references the information the
   * individual player knows.
   */
  private int[][][] phantomBoards;

  /**
   * This is the number of pieces each player has placed that the other doesn't
   * know where they are.
   */
  private int[] phantomPieceCount;

  /**
   * This is the number of locations each player has available to try and
   * place a piece.
   */
  private int[] phantomEmptyCount;

  /**
   *
   */
  private int[] phantomto_move;

  /**
   *
   * @param m
   * @param n
   * @param k
   */
  public phantommnkBoard(int m, int n, int k) {
    boardX = m;
    boardY = n;
    goalK = k;
    board = new int[boardX][boardY];
    phantomBoards = new int[2][boardX][boardY];
    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        board[x][y] = EMPTY_SQUARE;
        phantomBoards[PLAYER_BLACK][x][y] = EMPTY_SQUARE;
        phantomBoards[PLAYER_WHITE][x][y] = EMPTY_SQUARE;
      }
    }
    phantomPieceCount = new int[2];
    phantomEmptyCount = new int[2];
    phantomPieceCount[PLAYER_BLACK] = phantomPieceCount[PLAYER_WHITE] = 0;
    phantomEmptyCount[PLAYER_BLACK] = phantomEmptyCount[PLAYER_WHITE] = boardX * boardY;
    phantomto_move = new int[2];
    phantomto_move[PLAYER_BLACK] = phantomto_move[PLAYER_WHITE] = PLAYER_BLACK;
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


  /**
   * The possible number of events is the number of combinations
   * of opponent piece placement that we don't know about.
   * empty Choose opponentUnknown
   *
   * @param perspective the player that is asking
   * @return the number of combinations
   */
  public int getEventCount(int perspective) {

    long result = Util.combinationTotal(phantomEmptyCount[perspective],phantomPieceCount[perspective]);

    return (int)result;
  }

  /**
   * For this game, once we select a configuration, there won't
   * be a repeat of the uncertainty. As such the phantom piece count
   * goes to 0.
   *
   * @param perspective
   * @param index
   * @return the probability of this event.
   */
  public StochasticEvent makeEvent(int perspective, int index) {
    int count = 0;
    int curr = 0;
    phantommnkEvent ev = new phantommnkEvent();

    int [] chosen = Util.combination(index, phantomEmptyCount[perspective], phantomPieceCount[perspective]);

    ev.phantomEmptyCount = phantomEmptyCount[perspective];
    ev.phantomPieceCount = phantomPieceCount[perspective];
    ev.setEventProbability(0.0);
    ev.changedCellX = new int[chosen.length];
    ev.changedCellY = new int[chosen.length];

    // if no event to create, return.
    if (chosen[0] == 0) {
      ev.setEventProbability(1.0);
      return ev;
    }

    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        if (phantomBoards[perspective][x][y] == EMPTY_SQUARE) {
          count++;
          if (count == chosen[curr]) {
            ev.changedCellX[curr] = x;
            ev.changedCellY[curr] = y;

            curr++;
            count = 0;
            phantomBoards[perspective][x][y] = opponent(perspective);
            if (curr == phantomPieceCount[perspective]) { // we got the last one.
              // We have made a choice on the board configuration, there isn't any more
              // randomness.

              // We need to return the probability of this event occurring
              // for this game, the simplest approach is to just return a uniform
              // distribution over the chance nodes: 1/emptyCunknown
              ev.setEventProbability(1.0/(Util.combinationTotal(phantomEmptyCount[perspective],phantomPieceCount[perspective])));

              // And, the phantom empty squares goes to the actual empty squares and the
              // phantom piece count goes to 0.
              phantomEmptyCount[perspective] = phantomEmptyCount[perspective] - phantomPieceCount[perspective];
              phantomPieceCount[perspective] = 0;
              return ev;
            }
          }
        }
      }
    } // how am I going to reverse this if I zero the piece counts? ...

    return ev;
  }

  public void revealAll() {
    for (int x = 0; x < boardX; x++) {
      for (int y = 0; y < boardY; y++) {
        phantomBoards[PLAYER_BLACK][x][y] = board[x][y];
        phantomBoards[PLAYER_WHITE][x][y] = board[x][y];
      }
    }
  }
  public boolean reverseEvent(int perspective, int index, StochasticEvent event) {
    int count = 0;
    int curr = 0;
    phantommnkEvent ev = (phantommnkEvent)event;

    int [] chosen = Util.combination(index, ev.phantomEmptyCount, ev.phantomPieceCount);
    // if no event to reverse then return.
    if (chosen[0] == 0)
      return true;

    for (int i = 0; i < chosen.length; i++ ) {
      phantomBoards[perspective][ev.changedCellX[i]][ev.changedCellY[i]] = EMPTY_SQUARE;
    }

    phantomEmptyCount[perspective] = ev.phantomEmptyCount;
    phantomPieceCount[perspective] = ev.phantomPieceCount;
    // This doesn't work. It doesn't consider pieces that we have already
    // revealed.
/*    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        if (phantomBoards[perspective][x][y] == opponent(perspective) ||
            phantomBoards[perspective][x][y] == EMPTY_SQUARE) {
          count++;
          if (count == chosen[curr]) {
            curr++;
            count = 0;
            phantomBoards[perspective][x][y] = EMPTY_SQUARE;
            phantomPieceCount[perspective]++;
            phantomEmptyCount[perspective]++;
            if (curr == phantomPieceCount[perspective]) { // we got the last one.
              // We have made a choice on the board configuration, there isn't any more
              // randomness.

              return true;
            }
          }
        }
      }
    }*/

    return true;
  }


  /**
   *
   * @param perspective
   * @return
   */
  public Move generateMoves(int perspective) {
    mnkMove result = null;

    for (int x = 0; x < boardX; x++) {
      for (int y = 0; y < boardY; y++) {
        if (phantomBoards[perspective][x][y] == EMPTY_SQUARE) {
          mnkMove move = new mnkMove(x, y, phantomto_move[perspective]);
          move.next = result;
          result = move;
        }
      }
    }
    return result;
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
   * This has to update the actual board in mnkBoard as well as
   * update each player's perspective data. It is called when a
   * move is made - NOT during search.
   *
   * @return
   */
  @Override
  public boolean makeMove(Move m) {

    mnkMove move = (mnkMove)m;
    if (phantomBoards[to_move][move.x][move.y] == EMPTY_SQUARE) { // the player thinks the square is empty, double check...
      if (getPlayerAtLocation(move.x,move.y) == opponent(to_move)) { // if the square is already occupied...
        // reduce this player's 'unknown location' piece count.
        phantomPieceCount[to_move]--;
        // and update the board with the information that the opponent is in this location
        phantomBoards[to_move][move.x][move.y] = opponent(to_move);
        // the true board state doesn't change.
        phantomEmptyCount[to_move]--;
        to_move = opponent(to_move);
        return false; // returning false to notify the caller that this was a revealing move.
      }
      else { // the square is empty. Place the player's piece
        phantomBoards[to_move][move.x][move.y] = to_move;
        phantomto_move[PLAYER_BLACK] = phantomto_move[PLAYER_WHITE] = opponent(to_move);
        // And update the opponents 'unknown location' piece count.
        phantomPieceCount[opponent(to_move)]++;
        // The true board state now changes
        setPlayerAtLocation(move.x, move.y, to_move);
      }

      phantomEmptyCount[to_move]--;
      to_move = opponent(to_move);
      return true;
    }

    return false;
  }

  /**
   * This has to update the actual board in mnkBoard as well as
   * update each player's perspective data. It is called when a
   * move is reversed - NOT during search. Shouldn't be needed.
   *
   * @return
   */
  @Override
  public boolean reverseMove(Move m) {
    mnkMove move = (mnkMove)m;

    if (phantomBoards[move.player][move.x][move.y] == move.player) { // the player filled this square
      phantomPieceCount[opponent(move.player)]--;
      phantomBoards[move.player][move.x][move.y] = EMPTY_SQUARE;
      setPlayerAtLocation(move.x, move.y, EMPTY_SQUARE);
    } else if (phantomBoards[move.player][move.x][move.y] == opponent(move.player)) {
      // the player learned the opponent was in this square
      phantomPieceCount[move.player]++;
      phantomBoards[move.player][move.x][move.y] = EMPTY_SQUARE;
    }
    phantomEmptyCount[to_move]--;
    to_move = opponent(to_move);

    return true;
  }


  /**
   * This has to update the actual board in mnkBoard as well as
   * update each player's perspective data. It is called when a
   * move is made - NOT during search.
   *
   * @return
   */
  public boolean makeMove(int perspective, Move m) {
    mnkMove move = (mnkMove)m;

    if (phantomBoards[perspective][move.x][move.y] == EMPTY_SQUARE) { // the player thinks the square is empty, double check...
      // the square is empty. Place the player's piece
      phantomBoards[perspective][move.x][move.y] = phantomto_move[perspective];
      phantomto_move[perspective] =  opponent(phantomto_move[perspective]);
      // And update the opponents 'unknown location' piece count.
      phantomEmptyCount[perspective]--;
      return true;
    }

    return false;
  }

  /**
   * This has to update the actual board in mnkBoard as well as
   * update each player's perspective data. It is called when a
   * move is reversed - NOT during search. Shouldn't be needed.
   *
   * @return
   */
  public boolean reverseMove(int perspective, Move m) {
    mnkMove move = (mnkMove)m;

    if (phantomBoards[perspective][move.x][move.y] != EMPTY_SQUARE) { // the player filled this square
      phantomEmptyCount[perspective]++;
      phantomBoards[perspective][move.x][move.y] = EMPTY_SQUARE;
      phantomto_move[perspective] = opponent(phantomto_move[perspective]);
      return true;
    }

    return false;
  }

  public int endGame(int perspective) {

    int winner = getWinnerInRows(perspective);
    if (winner != GAME_CONTINUE)
      return winner;
    winner = getWinnerInColumns(perspective);
    if (winner != GAME_CONTINUE)
      return winner;
    winner = getWinnerInDiagonals(perspective);
    if (winner != GAME_CONTINUE)
      return winner;

    // Now we need to check if there are empty positions, otherwise it is a draw
    for (int i = 0; i < boardX; ++i)
      for (int j = 0; j < boardY; ++j)
        if (phantomBoards[perspective][i][j] == EMPTY_SQUARE)
          return GAME_CONTINUE;

    return GAME_DRAW;
  }

  /**
   * Returns a string of what the board look like from the 'perspective'
   *
   * @param perspective the player we are interested in
   * @return the board perspective player sees
   */
  public String toString(int perspective) {
    String result = new String();

    for (int x = 0; x < boardX; x++ ){
      for (int y = 0; y < boardY; y++ ) {
        if (phantomBoards[perspective][x][y] == PLAYER_WHITE)
          result = result.concat("O");
        else if (phantomBoards[perspective][x][y] == PLAYER_BLACK)
          result = result.concat("X");
        else
          result = result.concat(" ");
      }
      result = result.concat("\n");
    }

    return result;

  }

  public int getCurrentPlayer(int perspective) {
    return phantomto_move[perspective];
  }

  public int getPlayerAtLocation(int perspective, int x, int y) {
    return phantomBoards[perspective][x][y];
  }

  /**
   * Simple k in a row heuristic. Looks for the largest number of pieces (self and opponent)
   * on each row, column and diagonal. Then weights each as 1/4 and returns the result.
   * Doesn't take into consideration longest runs of pieces and associated relevant items.
   *
   * @param perspective
   * @return
   */
  public double heuristicEvaluation(int perspective) {
    int selfTally = 0;
    int max = -boardY;
    int min = boardY;
    double result;

    for (int x =0; x < boardX; x++) {
      for (int y = 0; y < boardY; y++) {
        if (phantomBoards[perspective][x][y] == perspective) {
          selfTally++;
        } else if (phantomBoards[perspective][x][y] == opponent(perspective)) {
          selfTally--;
        }
      }
      max = Math.max(selfTally, max); // best column for self
      min = Math.min(selfTally,min);  // best column for opponent
      selfTally = 0;
    }
    result = 0.25*((double)max/boardY - (double)min/boardY);

    max = -boardX;
    min = boardX;
    for (int y = 0; y < boardY; y++) {
      for (int x =0; x < boardX; x++) {
        if (phantomBoards[perspective][x][y] == perspective)
          selfTally++;
        else if (phantomBoards[perspective][x][y] == opponent(perspective))
          selfTally--;
      }
      max = Math.max(selfTally, max); // best row for self
      min = Math.min(selfTally,min);  // best row for opponent
      selfTally = 0;
    }
    result = result + 0.25*((double)max/boardX - (double)min/boardX);

    // Check the lower-left to upper-right diagonals -> /
    max = -Math.max(boardX, boardY);
    min = -max;
    for(int y = boardY - goalK; y >= 0; y--) {
      for (int x = 0; x < boardX; x++) {
        selfTally = 1;
        for (int diag = 0; ((x+diag) < boardX && (y+diag) < boardY); diag++) {
          if (phantomBoards[perspective][x+diag][y+diag] == perspective)
            selfTally++;
          else if (phantomBoards[perspective][x+diag][y+diag] == opponent(perspective))
            selfTally--;
        }
      }
      max = Math.max(selfTally, max); // best diagonal for self
      min = Math.min(selfTally,min);  // best diagonal for opponent
      selfTally = 0;
    }
    result = result + 0.25*((double)max/(Math.max(boardX, boardY)) - (double)min/(Math.max(boardX,boardY)));

    // Check the upper-left to lower-right diagonals -> \
    max = -Math.max(boardX, boardY);
    min = -max;
    for (int x = boardX - goalK; x >= 0; x--) {
      for (int y = boardY -1 ; y > 0; y--) {
        selfTally = 1;
        for (int diag = 0; ((y-diag) > 0 && (x+diag+1) < boardX); diag++) {
          if (phantomBoards[perspective][x+diag][y-diag] == perspective)
            selfTally++;
          else if (phantomBoards[perspective][x+diag][y-diag] == opponent(perspective))
            selfTally--;
        }
      }
      max = Math.max(selfTally, max); // best diagonal for self
      min = Math.min(selfTally,min);  // best diagonal for opponent
      selfTally = 0;
    }
    result = result + 0.25*((double)max/(Math.max(boardX, boardY)) - (double)min/(Math.max(boardX,boardY)));

    return result;
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

  public void setPlayerAtLocation(int perspective, int x, int y, int side) {
    phantomBoards[perspective][x][y] = side;
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
    for (int row = 0; row < boardY; row++) {
      int count = 1;
      // We will compare current element with the previous
      for (int column = 1; column < boardX; column++) {
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
   * Checks each of the board rows for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInRows(int perspective) {
    // Check rows and see if there are k pieces of the same color
    for (int row = 0; row < boardY; row++) {
      int count = 1;
      // We will compare current element with the previous
      for (int column = 1; column < boardX; column++) {
        if (phantomBoards[perspective][row][column] != EMPTY_SQUARE &&
            phantomBoards[perspective][row][column] == phantomBoards[perspective][row][column-1])
          ++count;
        else
          count = 1;

        // Check if there are k in a row.
        if (count >= goalK) {
          // Return color of the winner
          return phantomBoards[perspective][row][column];
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
    for (int column = 0; column < boardX; column++) {
      int count = 1;
      // We will compare current element with the previous
      for (int row = 1; row < boardY; row++) {
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
   * Checks each of the board columns for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInColumns(int perspective) {
    // Check rows and see if there are k pieces of the same color
    for (int column = 0; column < boardX; column++) {
      int count = 1;
      // We will compare current element with the previous
      for (int row = 1; row < boardY; row++) {
        if (phantomBoards[perspective][row][column] != EMPTY_SQUARE &&
            phantomBoards[perspective][row][column] == phantomBoards[perspective][row-1][column])
          ++count;
        else
          count = 1;

        // Check if there are k in a row.
        if (count >= goalK) {
          // Return color of the winner
          return phantomBoards[perspective][row][column];
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

  /**
   * Checks each of the board diagonals for k pieces in a row.
   *
   * @return winning player {PLAYER_BLACK, PLAYER_WHITE} or GAME_CONTINUE
   */
  private int getWinnerInDiagonals(int perspective) {
    int count;

    // Check the lower-left to upper-right diagonals -> /
    for(int y = boardY - goalK; y >= 0; y--) {
      for (int x = 0; x < boardX; x++) {
        count = 1;
        for (int diag = 0; ((x+diag+1) < boardX && (y+diag+1) < boardY); diag++) {
          if ((phantomBoards[perspective][x+diag][y+diag] != EMPTY_SQUARE) &&
              (phantomBoards[perspective][x+diag][y+diag] == phantomBoards[perspective][x+diag+1][y+diag+1]))
            count++;
          else
            count = 1;
          if ( count >= goalK) {
            return phantomBoards[perspective][x+diag][y+diag];
          }
        }
      }
    }

    // Check the upper-left to lower-right diagonals -> \
    for (int x = boardX - goalK; x >= 0; x--) {
      for (int y = boardY -1 ; y > 0; y--) {
        count = 1;
        for (int diag = 0; ((y-diag) > 0 && (x+diag+1) < boardX); diag++) {
          if ((phantomBoards[perspective][x+diag][y-diag] != EMPTY_SQUARE) &&
              (phantomBoards[perspective][x+diag][y-diag] == phantomBoards[perspective][x+diag+1][y-diag-1]))
            count++;
          else
            count = 1;
          if (count >= goalK)
            return phantomBoards[perspective][x+diag][y-diag];
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
