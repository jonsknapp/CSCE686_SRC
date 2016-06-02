package AbstractGames;

/**
 *
 */
public class ExpectiminimaxSearch<BOARD extends StochasticBoard,MOVE extends Move> implements Search<BOARD, MOVE>{

  BOARD board;
  int maxPlayer;

  static final double INF = 1.0;

  int totalNodesSearched;
  int totalLeafNodes;

  /**
   *
   */
  public ExpectiminimaxSearch() {
    totalNodesSearched = 0;
    totalLeafNodes = 0;
  }

  /**
   *
   * @param board Game state
   * @param depth Search depth
   * @return
   */
  @Override
  public MOVE findBestMove(BOARD board, int depth) {
    MOVE best_move = null;
    int runningNodeTotal = 0;
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0;
    long currentPeriod;
    long previousPeriod = 0;

    this.board = board;
    maxPlayer = board.getCurrentPlayer();

    totalNodesSearched = totalLeafNodes = 0;

    best_move = Expectiminimax(depth); // Min-Max alpha beta with transposition tables

    elapsedTime = System.currentTimeMillis()-startTime;
    currentPeriod = elapsedTime-previousPeriod;

    runningNodeTotal += totalNodesSearched;
    System.out.println("Depth: " + depth +" Time: " + elapsedTime/1000.0 + " " + currentPeriod/1000.0 + " Nodes Searched: " +
        totalNodesSearched + " Leaf Nodes: " + totalLeafNodes);


    System.out.println("Nodes per Second = " + runningNodeTotal/(elapsedTime/1000.0));
    if (best_move == null ) {
      throw new Error ("No Move Available - Search Error!");
    }
    return best_move;
  }

  /**
   *
   * @param depth
   * @return
   */
  private MOVE Expectiminimax(int depth) {
    MOVE best_move = (MOVE)board.newMove();
    MOVE bestEUMove = (MOVE)board.newMove();
    MOVE opponent_move = null;
    MOVE best_opponent_move = null;
    double v;

    //First check if this is a frontier node, maximum depth?
    if (depth <= 0) {
      totalLeafNodes++;
      best_move.value = board.heuristicEvaluation(maxPlayer);
      return best_move; // If so, just return the evaluation of this board.
    }

    // Is this the end of the game?
    int game_state = board.endGame(maxPlayer);
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

    // How do I want to handle the chance events. Capture it in genMoves? but then
    // tracking the sum would be harder. What happens if the chance is
    // in multiple locations? And are there games with chance at
    // multiple spots? Do we even care?

    // There are two ways this can go:
    // 1) Chance is a die roll - in which case, we should only search AFTER the chance roll.
    // 2) Chance is a card deal - in which case, there are multiple worlds and we need to find
    //    the best move for the current probability distribution over the worlds.
    // Solutions:
    //   For both, we want to return one (1) best move.
    // So for 1, have the chance event call return just 1 option, make the option and do the search.
    // Then for both, we need to identify the one move with the highest expected utility - separate
    // from the best move from the internal search.

    double expectedUtility;
    long numberOfChanceEvents = board.getEventCount(maxPlayer);

    best_move.value = bestEUMove.value = -2.0;

    for (int i = 1; i <= numberOfChanceEvents; i++) {
      StochasticEvent event = board.makeEvent(maxPlayer,i);

      MOVE moves = (MOVE)board.generateMoves(maxPlayer); // No move ordering in this search

      expectedUtility = 0.0;
      // Iterate through each valid move, trying each one, and calculating results
      for ( MOVE m = moves; m != null; m = (MOVE) m.next ) {
        board.makeMove(maxPlayer,m);
        opponent_move = Expectiminimax(depth-1);
        v = -opponent_move.value;
        if (v > best_move.value) {
          best_move = m;
          best_move.value = v;
          best_opponent_move = opponent_move;
        }
        board.reverseMove(maxPlayer,m); //Undo the move we just tried.
      }
      expectedUtility += event.getEventProbability() * best_move.value;
      if (expectedUtility > bestEUMove.value) {
        bestEUMove = best_move;
        bestEUMove.value = expectedUtility;
      }

      board.reverseEvent(maxPlayer, i, event);
    }

    return (MOVE)bestEUMove;
  }
}
