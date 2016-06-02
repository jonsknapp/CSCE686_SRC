package AbstractGames;

/**
 *
 */
public class MinimaxSearch<BOARD extends Board, MOVE extends Move> implements Search<BOARD,MOVE> {
  BOARD board;
  int totalNodesSearched;
  int totalLeafNodes;
  static final double INF = 1000.0;

  public MinimaxSearch() {
    totalNodesSearched = 0;
    totalLeafNodes = 0;
  }

  public MOVE findBestMove(BOARD board, int depth) {
    MOVE best_move = null;
    int runningNodeTotal = 0;
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0;
    long currentPeriod;
    long previousPeriod = 0;
    int i = 1;

    this.board = board;

    // Including the iterative deepening for consistency.
    while ( i <= depth ) {
      totalNodesSearched = totalLeafNodes = 0;

      best_move = Minimax(i); // Min-Max alpha beta with transposition tables

      elapsedTime = System.currentTimeMillis()-startTime;
      currentPeriod = elapsedTime-previousPeriod;
      double rate = 0.0;
      if ( i > 3 && previousPeriod > 50 )
        rate = (currentPeriod - previousPeriod)/previousPeriod;
      previousPeriod = elapsedTime;

      runningNodeTotal += totalNodesSearched;
      System.out.println("Depth: " + i +" Time: " + elapsedTime/1000.0 + " " + currentPeriod/1000.0 + " Nodes Searched: " +
          totalNodesSearched + " Leaf Nodes: " + totalLeafNodes + " Rate: " + rate);

      // increment indexes;
      i =  i+2;
    }

    System.out.println("Nodes per Second = " + runningNodeTotal/(elapsedTime/1000.0));
    if (best_move == null ) {
      throw new Error ("No Move Available - Search Error!");
    }
    return best_move;
  }

  /**
   * This is an implementation of the Negamax algorithm
   *
   * @param depth Depth to search to
   * @return best move found at this node
   */
  @SuppressWarnings("unchecked")
  private MOVE Minimax(int depth) {
    totalNodesSearched++;
    MOVE best_move = (MOVE)board.newMove();
    MOVE opponent_move = null;
    MOVE best_opponent_move = null;
    double v;

    // This is the complete implementation of negamax
    //First check if this is a frontier node, maximum depth?
    if (depth <= 0) {
      totalLeafNodes++;
      best_move.value = board.heuristicEvaluation();
      return best_move; // If so, just return the evaluation of this board.
    }

    // Is this the end of the game?
    int game_state = board.endGame();
    if (game_state != Board.GAME_CONTINUE) {
      if (game_state == board.getCurrentPlayer()) { // Did this player win?
        best_move.value = (INF + depth);
        return best_move;
      }
      int []player = board.getPlayerList(); // Did an opponent win?
      for (int i = 0; i < player.length; i++) {
        if (game_state == player[i]) {
          best_move.value = -(INF + depth);
          return best_move;
        }
      }
      if (game_state == Board.GAME_DRAW) { // Game is a draw
        best_move.value = -(INF - depth);
        return best_move;
      }
    }

    // Get all valid moves for this board
    MOVE moves = (MOVE)board.generateMoves();
    if (moves == null) { // Do we have any moves that we can make? If not assume that we have to withdraw and lose.
      best_move.value = -(INF+depth);
      return best_move;
    }

    // Search the first generated move so that there is something valid in
    // best_move if we bug out early.
    board.makeMove(moves);
    best_opponent_move = opponent_move = Minimax(depth - 1);
    best_move = moves; // This gives a best move in case one is not found in time later.
    if (opponent_move != null )
      best_move.value = -opponent_move.value;
    board.reverseMove(moves);
    moves = (MOVE)moves.next;

    // Iterate through each valid move, trying each one, and calculating results
    for ( MOVE m = moves; m != null; m = (MOVE)m.next ) {
      board.makeMove(m);
      opponent_move = Minimax(depth-1);
      v = -opponent_move.value;
      if (v > best_move.value) {
        best_move = m;
        best_move.value = v;
        best_opponent_move = opponent_move;
      }
      board.reverseMove(m); //Undo the move we just tried.
    }

    best_move.next = best_opponent_move; // Track the move list.

    return best_move;
  }
}
