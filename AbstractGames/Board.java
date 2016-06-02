package AbstractGames;

/**
 *
 */

public abstract class Board {

  public static final int GAME_DRAW = -2;
  public static final int GAME_CONTINUE = -3;

  /**
   * This method should return all of the possible moves from the current game state
   */
  public abstract Move generateMoves();

  // Move this to a GUI interface:
   // This method should return all of the possible moves for a provided subspace of the game state
  //public abstract Move generateMovesFromLocation();

  /**
   * Execute the move m, updating the game state
   *
   * @param m the move to make
   * @return true if success, false if not.
   */
  public abstract boolean makeMove(Move m);

  /**
   * Reverse the move m, updating the game state
   *
   * @param m the move to reverse
   * @return true if success, false if not.
   */
  public abstract boolean reverseMove(Move m);

  /**
   * Check the end game condition, return player, draw, or in progress
   */
  public abstract int endGame();

  /**
   * Evaluate the winning potential of the current game state.
   *
   * Recommend overloading the constructor to receive a heuristic evaluator
   * that can be changed as needed - this will allow for a single board to use
   * a learned or static board evaluator.
   * An external board evaluator will require that getters be created for any
   * private data structures the heuristic would need.
   *
   * @return evaluation of the state
   */
  public abstract double heuristicEvaluation();

  /**
   * Assign a value to each move (Move.value) and return Util.QuickSort(move_list)
   *
   * Implementing moveOrdering and moveOrderingData are necessary to
   * achieve maximum benefits from searches that include pruning (i.e. Alpha Beta)
   *
   * @param move_list Moves to be rank ordered
   * @return sorted move_list
   */
  public abstract Move moveOrdering(Move move_list, int depth);

  /**
   * Move data structure update for moveOrdering use.
   *
   * @param best_move the move selected by the search
   * @param pruned true if the move in best_move was pruned
   */
  public abstract void moveOrderingData(Move best_move, int depth, boolean pruned);

  /**
   * Get the current game state's player identifier
   *
   * @return int constant
   */
  public abstract int getCurrentPlayer();

  /**
   *  Get an array of the current game state's opponents identifier
   *  Allows flexibility for more than 2 players
   *
   * @return list of opponent constants
   */
  public abstract int[] getPlayerList();

  public abstract String toString();
  public abstract void loadBoard(String boardString);

  // Overload the newMove item with a blank move constructor of a move for
  // the game being implemented (i.e. for TicTacToe, this would return a blank
  // TicTacToeMove).
  public abstract Move newMove();
  
  public abstract void printBoardRep();
}
