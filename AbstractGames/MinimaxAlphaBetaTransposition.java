package AbstractGames;

/**
 *
 */
public class MinimaxAlphaBetaTransposition<BOARD extends TranspositionBoard, MOVE extends Move> implements Search<BOARD,MOVE> {

  BOARD board;
  int totalNodesSearched;
  int totalLeafNodes;
  int transpositionTableHit;
  int actualHit;

  public MinimaxAlphaBetaTransposition() {
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
    while (i <= depth) {
      totalNodesSearched = totalLeafNodes = transpositionTableHit = actualHit = 0;

      best_move = MinimaxAB_TT(i, -2.0, 2.0); // Min-Max alpha beta

      elapsedTime = System.currentTimeMillis() - startTime;
      currentPeriod = elapsedTime - previousPeriod;
      double rate = 0.0;
      if (i > 3 && previousPeriod > 50)
        rate = (currentPeriod - previousPeriod) / previousPeriod;
      previousPeriod = elapsedTime;

      runningNodeTotal += totalNodesSearched;
      System.out.println("Depth: " + i + " Time: " + elapsedTime / 1000.0 + " " + currentPeriod / 1000.0 + " Nodes Searched: " +
          totalNodesSearched + " Leaf Nodes: " + totalLeafNodes + " Transposition Table Hits: " + transpositionTableHit + " actualHit: " + actualHit + " Rate: " + rate);

      // increment indexes;
      i = i + 2;
    }

    board.clearTranspositionTable();

    System.out.println("Nodes per Second = " + runningNodeTotal/(elapsedTime/1000.0));
    if (best_move == null ) {
      throw new Error ("No Move Available - Search Error!");
    }
    return best_move;
  }

  /**
   * Min-max alpha beta with transposition table.
   *
   * @param depth int the depth of the search to conduct
   * @return maximum heuristic board found value
   */
  MOVE MinimaxAB_TT(int depth, double alpha, double beta) {
    totalNodesSearched++;
    MOVE best_move = (MOVE)board.newMove();
    MOVE opponent_move = null;
    MOVE best_opponent_move = null;
    MOVE TransMove = null;
    boolean valid = false;
    boolean eval_is_exact = false;

    // This is the complete implementation of negamax with Alpha-Beta
    // and the transposition table optimization
    //First check if this is a frontier node, maximum depth?
    // Check if this is a frontier node, maximum depth?
    if (depth < 1) {
      totalLeafNodes++;
      best_move.value = board.heuristicEvaluation();
      board.RecordHash(depth, best_move.value, board.HASH_EXACT, null, board);
      return best_move; // If so, just return the evaluation of this board.
    }

    TransMove = (MOVE)board.ProbeMove();
    int flag = board.ProbeHash(depth, alpha, beta);
    if ( flag != -5 &&  board.moveIsValid(TransMove)) {
      TransMove.value = board.ProbeValue(depth);
      valid = true;
      transpositionTableHit++;
      if (flag == board.HASH_EXACT) { // hashExact
        actualHit++;
        return TransMove; // return this move
      }
      if (flag == board.HASH_ALPHA && TransMove.value > alpha) // hashAlpha
        alpha = TransMove.value;
      if (flag == board.HASH_BETA && beta > TransMove.value) // hashBeta
        beta = TransMove.value;
      if (alpha >= beta) { // alpha and beta have crossed return this move
        return TransMove;
      }
    }
    else
      TransMove = null;

    // Is this the end of the game?
    int game_state = board.endGame();
    if (game_state != Board.GAME_CONTINUE) {
      if (game_state == board.getCurrentPlayer()) { // Did this player win?
        best_move.value = 1.0+depth/1.0;
        return best_move;
      }
      int []player = board.getPlayerList(); // Did an opponent win?
      for (int i = 0; i < player.length; i++) {
        if (game_state == player[i]) {
          best_move.value = -(1.0+depth/1.0);
          return best_move;
        }
      }
      if (game_state == Board.GAME_DRAW) { // Game is a draw
        best_move.value = -(1.0+depth/1.0);
        return best_move;
      }
    }

    Move moves = board.generateMoves();  // Get all valid moves for this board
    if (moves == null) { // Do we have any moves that we can make? If not assume that we have to withdraw and lose.
      best_move.value = -(1.0+depth/1.0);
      return best_move;
    }
    moves = board.moveOrdering(moves, depth);

    // If we got a valid transposition table move that wasn't an exact hash,
    // search it's move first.
    if (valid) { // apply the transposition move first
      board.makeMove( TransMove );
      best_opponent_move = opponent_move = MinimaxAB_TT(depth - 1, -beta, -alpha); // search deeper
      TransMove.value = -opponent_move.value; // update the score and if > beta return
      best_move = TransMove;
      best_move.value = alpha;
      if ( opponent_move != null )
        best_move.value = - opponent_move.value;
      board.reverseMove(TransMove);
    }
    // Otherwise search the first generated move so that there is something in
    // best_move if we bug out early. Only do this if we haven't searched the
    // transposition table move.
    if (!valid ) {
      board.makeMove(moves);
      best_opponent_move = opponent_move = MinimaxAB_TT(depth - 1, -beta, -alpha);
      best_move = (MOVE)moves; // This gives a best move in case one is not found in time later.
      best_move.value = alpha;
      if (opponent_move != null )
        best_move.value = -opponent_move.value;
      board.reverseMove(moves);
      moves = moves.next;
    }

    // Iterate through each valid move, trying each one, and calculating results
    for ( Move m = moves; m != null; m = m.next ) {
      // Skip the transposition table move because we have already searched it.
      if ( m.equals(TransMove) )
        continue;
      if ( best_move.value >= beta ) {//this whole node is trash, b/c the opponent won't allow it
        board.RecordHash(depth, best_move.value, board.HASH_BETA, best_move, board);
        board.moveOrderingData(best_move, depth, true);
        return best_move;
      }
      board.makeMove(m);
      if ( alpha < best_move.value )
        alpha = best_move.value;
      Move opponent = MinimaxAB_TT(depth - 1, -beta, -alpha);
      double v = -opponent.value;
      if (v > alpha) {
        alpha = v;
        best_move = (MOVE)m;
        best_move.value = v;
        best_opponent_move = opponent_move;
        eval_is_exact = true;
      }
      board.reverseMove(m); //Undo the move we just tried.
    }

    best_move.next = best_opponent_move; // Need to track the move list for the KillerMove ordering.
    if (eval_is_exact)
      board.RecordHash(depth, alpha, board.HASH_EXACT, best_move, board);
    else
      board.RecordHash(depth, best_move.value, board.HASH_ALPHA, best_move, board);
    board.moveOrderingData(best_move, depth, false);

    return best_move;
  }

}
