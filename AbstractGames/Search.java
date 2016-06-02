package AbstractGames;

/**
 * Search interface. Each search algorithm should implement this interface.
 * MOVE is a class that extends Move. BOARD is a class that extends Board.
 * The Board is the game state, and Moves are the player actions.
 */
public interface Search<BOARD, MOVE> {
  /**
   * Find the best move for the state in board.
   *
   * @param board Game state
   * @param depth Search depth
   * @return the best move
   */
  public  MOVE findBestMove(BOARD board, int depth);
}
