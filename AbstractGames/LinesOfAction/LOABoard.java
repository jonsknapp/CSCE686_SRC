package AbstractGames.LinesOfAction;

import AbstractGames.*;

/**
 *
 */
public class LOABoard extends TranspositionBoard {

  static final int BOARD_SIZE = 8;
  static final int MAX_DEPTH = 250;

  static final int EMPTY_SQUARE = -1;
  static final int PLAYER_WHITE = 0;
  static final int PLAYER_BLACK = 1;

  /**
   * The game board
   */
  int square[][] = new int[BOARD_SIZE][BOARD_SIZE];

  /**
   * These four arrays contain the piece count on the vertical, horizontal,
   * and both diagonals - speeds move generation as per Winands.
   */
  int vertical_count[] = new int[BOARD_SIZE];
  int horizontal_count[] = new int[BOARD_SIZE];
  int forward_diag_count[] = new int [BOARD_SIZE+BOARD_SIZE-1];
  int back_diag_count[] = new int [BOARD_SIZE+BOARD_SIZE-1];

  int quad[][][] = new int [2][BOARD_SIZE+1][BOARD_SIZE+1];
  int quadcount[][] = new int [2][6];

  // Maintaining the piece lists in addition to the boards really speeds up the
  // heuristic calculation.
  LOAPiece piece_list[] = new LOAPiece[2];

  private StaticBoardEvaluator staticBoardEvaluator;

  //////////////////////////////
  //Begin feature constants for static board evaluator
  /////////////////////////////
  private int numWeights = 6;
  static final double MIDPOINT = 0.5*LOABoard.BOARD_SIZE-0.5; //1.5;
  static final double CENTER_VALUE = MIDPOINT*MIDPOINT; //2.25;
  static final int BOARD_INDEX = LOABoard.BOARD_SIZE-1;
  //////////////////////////////
  //End feature constants for static board evaluator
  /////////////////////////////

  private int game_state = GAME_CONTINUE;
  private int to_move = PLAYER_BLACK;

  /**
   * Board statistics
   */
  public int moveCount;

  /**
   * Data structures for move ordering.
   * killer_moves is the best move at each depth found so far
   * history_moves is the best move for every search performed rated by depth of search
   */
  LOAMove killer_moves[] = new LOAMove[MAX_DEPTH];
  int history_moves[][][][][] = new int[2][BOARD_SIZE][BOARD_SIZE][BOARD_SIZE][BOARD_SIZE];

  public Transposition transpositionTable;

  // Some search flags
  boolean TranspositionTable_Yes = true; // true = use transposition tables
  boolean KillerMoves_Yes = true;        // Use the killer move during move ordering
  boolean OutsideMoves_Yes = true;      // Don't count moves to the outside as high during move ordering
  boolean HistoryMoves_Yes = true;       // Use the history value as the move ordering value


  final static private boolean debug_move_ok = false;

  /**
   * Constructor.
   *
   * @param h_value static board evaluator object
   */
  public LOABoard(StaticBoardEvaluator h_value) {
    staticBoardEvaluator = h_value;
    initialize();
  }

  /**
   * Initializes the board, setting all pieces in the starting configuration.
   * This is called on Board creation as well as whenever the game restarts to
   * reset the entire board state.
   *
   */
  public void initialize() {
    to_move = PLAYER_BLACK;
    game_state = GAME_CONTINUE;
    piece_list[PLAYER_BLACK] = null;
    piece_list[PLAYER_WHITE] = null;
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        square[i][j] = EMPTY_SQUARE;
    for (int i = 1; i < (BOARD_SIZE-1); i++) {
      square[i][0] = PLAYER_BLACK;
      square[i][(BOARD_SIZE-1)] = PLAYER_BLACK;
      piece_list[PLAYER_BLACK] = new LOAPiece(i,0, PLAYER_BLACK, piece_list[PLAYER_BLACK], null);
      piece_list[PLAYER_BLACK] = new LOAPiece(i,(BOARD_SIZE-1), PLAYER_BLACK, piece_list[PLAYER_BLACK], null);
      square[0][i] = PLAYER_WHITE;
      square[(BOARD_SIZE-1)][i] = PLAYER_WHITE;
      piece_list[PLAYER_WHITE] = new LOAPiece(0,i, PLAYER_WHITE, piece_list[PLAYER_WHITE], null);
      piece_list[PLAYER_WHITE] = new LOAPiece((BOARD_SIZE-1),i, PLAYER_WHITE, piece_list[PLAYER_WHITE], null);
      vertical_count[i] = 2;
      horizontal_count[i] = 2;
      forward_diag_count[i] = 2;
      forward_diag_count[i+(BOARD_SIZE-1)] = 2;
      back_diag_count[i] = 2;
      back_diag_count[i+(BOARD_SIZE-1)] = 2;
    }
    recountQuads();
    vertical_count[0] = BOARD_SIZE-2;
    vertical_count[(BOARD_SIZE-1)] = BOARD_SIZE-2;
    horizontal_count[0] = BOARD_SIZE-2;
    horizontal_count[(BOARD_SIZE-1)] = BOARD_SIZE-2;
    forward_diag_count[0] = 0;
    forward_diag_count[(BOARD_SIZE-1)] = 0;
    forward_diag_count[(BOARD_SIZE-1)*2] = 0;
    back_diag_count[0] = 0;
    back_diag_count[(BOARD_SIZE-1)] = 0;
    back_diag_count[(BOARD_SIZE-1)*2] = 0;

    //set transposition table to start position
    if (TranspositionTable_Yes) {
      transpositionTable = new Transposition();
      transpositionTable.initialize();
    }
  }

  /**
   * Creates a list of all of the possible valid moves and stores them in a
   * List. Used by search to get a list of all possible moves.
   *
   * @return List list of all valid moves.
   */
  public Move generateMoves(){
    LOAMove result = null;
    for ( LOAPiece p = piece_list[to_move]; p != null; p = p.next ) {
      int i = p.x;
      int j = p.y;
      if ( i < 0 || j < 0 || square[p.x][p.y] != to_move ) // Sometimes the piece list gets corrupted trying to stem the tide because I can't track the problem.
        continue;
      int pieces = vertical_count[i];
      if (j - pieces >= 0 && square[i][j - pieces] != to_move)
        result = blocked(result, i, j, 0, -1, pieces, p);
      if (j + pieces < BOARD_SIZE && square[i][j + pieces] != to_move)
        result = blocked(result, i, j, 0, 1, pieces, p);
      pieces = horizontal_count[j];
      if (i - pieces >= 0 && square[i - pieces][j] != to_move)
        result = blocked(result, i, j, -1, 0, pieces, p);
      if (i + pieces < BOARD_SIZE && square[i + pieces][j] != to_move)
        result = blocked(result, i, j, 1, 0, pieces, p);
      pieces = forward_diag_count[i +(BOARD_SIZE-1-j)];
      if (i + pieces < BOARD_SIZE && j + pieces < BOARD_SIZE && square[i + pieces][j + pieces] != to_move)
        result = blocked(result, i, j, 1, 1, pieces, p);
      if (i - pieces >= 0 && j - pieces >= 0 && square[i-pieces][j-pieces] != to_move)
        result = blocked(result, i, j, -1, -1, pieces, p);
      pieces = back_diag_count[i + j];
      if (i + pieces < BOARD_SIZE && j - pieces >= 0 && square[i + pieces][j - pieces] != to_move)
        result = blocked(result, i, j, 1, -1, pieces, p);
      if (i-pieces >= 0 && j+pieces < BOARD_SIZE && square[i - pieces][j + pieces] != to_move)
        result = blocked(result, i, j, -1, 1, pieces, p);
    }
    return result;
  }

  /**
   * Creates a list of all possible valid moves from location [i,j] and stores
   * them in a vector. Used by the GUI when a player selects a piece.
   *
   * @param i int x location value
   * @param j int y location value
   * @return Vector list of all valid moves from [i,j]
   */
  public LOAMove generateMovesFromLocation(int i, int j){
    LOAMove result = null;

    LOAPiece p = findPiece( i, j, to_move);
    if (square[i][j] ==  to_move){
      int pieces = vertical_count[i];
      if (j - pieces >= 0 && square[i][j - pieces] != to_move)
        result = blocked(result, i, j, 0, -1, pieces, p);
      if (j + pieces < BOARD_SIZE && square[i][j + pieces] != to_move)
        result = blocked(result, i, j, 0, 1, pieces, p);
      pieces = horizontal_count[j];
      if (i - pieces >= 0 && square[i - pieces][j] != to_move)
        result = blocked(result, i, j, -1, 0, pieces, p);
      if (i + pieces < BOARD_SIZE && square[i + pieces][j] != to_move)
        result = blocked(result, i, j, 1, 0, pieces, p);
      pieces = forward_diag_count[i +(BOARD_SIZE-1-j)];
      if (i + pieces < BOARD_SIZE && j + pieces < BOARD_SIZE && square[i + pieces][j + pieces] != to_move)
        result = blocked(result, i, j, 1, 1, pieces, p);
      if (i - pieces >= 0 && j - pieces >= 0 && square[i-pieces][j-pieces] != to_move)
        result = blocked(result, i, j, -1, -1, pieces, p);
      pieces = back_diag_count[i + j];
      if (i + pieces < BOARD_SIZE && j - pieces >= 0 && square[i + pieces][j - pieces] != to_move)
        result = blocked(result, i, j, 1, -1, pieces, p);
      if (i-pieces >= 0 && j+pieces < BOARD_SIZE && square[i - pieces][j + pieces] != to_move)
        result = blocked(result, i, j, -1, 1, pieces, p);
    }

    return result;
  }

  /**
   * Update the game state based upon the move submitted.
   *
   * @param move an LOAMove
   * @return true if complete
   */
  public boolean makeMove(Move move){
    // Check to see if this was a capture
    LOAMove m= (LOAMove)move;
    int x1 = m.x1;
    int y1 = m.y1;
    int x2 = m.x2;
    int y2 = m.y2;
    m.captured = square[x2][y2];
    int side = square[x1][y1];
    // reduce quad counts for the from and to locations then update the quads
    // and recalculate the quad values.
    subtractQuad_numbers(x1, y1, side);
    subtractQuad_numbers(x2, y2, side);

    // If we are only moving one square up, down or left, right need to ensure
    // that the quadcounts do not get off.
    LOAPiece quad_list = null;
    if (Math.abs(x1 - x2) == 1 && (y1 - y2) == 0) {
      int max_row = Math.max(x1, x2);
      quad_list = new LOAPiece(max_row, y1, side);
      quad_list.next = new LOAPiece(max_row, y1 + 1, side);
    } else if ((x1 - x2) == 0 && Math.abs(y1 - y2) == 1) {
      int max_col = Math.max(y1, y2);
      quad_list = new LOAPiece(x1, max_col, side);
      quad_list.next = new LOAPiece(x1 + 1, max_col, side);
    }
    for (LOAPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]++;

    // SIMPLE
    m.piece = findPiece( x1, y1, side);
    m.piece.x = m.x2;
    m.piece.y = m.y2;
    if (TranspositionTable_Yes)
      transpositionTable.makeMove(x1, y1, x2, y2, (m.captured != EMPTY_SQUARE), side);
    square[x2][y2] = square[x1][y1];
    square[x1][y1] = EMPTY_SQUARE;
    subtractLines(x1, y1);

    // Now update the quad information
    removeQuad(x1, y1, side);
    addQuad(x2, y2, side);
    // If this was a capture do the same for the opponent
    if (m.captured != EMPTY_SQUARE) {
      piece_list[m.captured] = deletePiece(piece_list[m.captured], x2, y2, m);
      subtractQuad_numbers(x2, y2, m.captured);
      removeQuad(x2, y2, m.captured);
      addQuad_numbers(x2, y2, m.captured);
    } else
      addLines(x2, y2);
    // Here is the recalculating of the quad counts
    addQuad_numbers(x2, y2, side);
    addQuad_numbers(x1, y1, side);
    for (LOAPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]--;
    to_move = opponent(to_move);
//    if (debug_quads)
//      quadCheck();

    return true;
  }

  /**
   * Execute the removal of an action
   *
   * @param move Move the move to remove, should be the last move executed.
   */
  public boolean reverseMove(Move move){
    LOAMove m = (LOAMove)move;
    int x1 = m.x1;
    int y1 = m.y1;
    int x2 = m.x2;
    int y2 = m.y2;
    to_move = opponent(to_move);
    m.piece.x = x1;
    m.piece.y = y1;
    square[x1][y1] = square[x2][y2];
    square[x2][y2] = m.captured;
    int side = square[x1][y1];
    if (TranspositionTable_Yes)
      transpositionTable.makeMove(x1, y1, x2, y2, (m.captured != EMPTY_SQUARE), side);
    addLines(m.x1, m.y1);
    // If we are only moving one square up, down or left, right need to ensure
    // that the quadcounts do not get off.
    LOAPiece quad_list = null;
    if (Math.abs(x1 - x2) == 1 && (y1 - y2) == 0) {
      int max_row = Math.max(x1, x2);
      quad_list = new LOAPiece(max_row, y1, side);
      quad_list.next = new LOAPiece(max_row, y1 + 1, side);
    } else if ((x1 - x2) == 0 && Math.abs(y1 - y2) == 1) {
      int max_col = Math.max(y1, y2);
      quad_list = new LOAPiece(x1, max_col, side);
      quad_list.next = new LOAPiece(x1 + 1, max_col, side);
    }
    for (LOAPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]++;

    // QUAD
    subtractQuad_numbers(x1, y1, side);
    subtractQuad_numbers(x2, y2, side);

    // Now update the quad information, will update through the methods
    // instead of expending memory
    removeQuad(x2, y2, side);
    addQuad(x1, y1, side);

    if (m.captured != EMPTY_SQUARE) {
      piece_list[m.captured] = addPiece(piece_list[m.captured], m.captured_piece);
      subtractQuad_numbers(x2, y2, m.captured);
      addQuad(x2, y2, m.captured);
      addQuad_numbers(x2, y2, m.captured);
    } else
      subtractLines(x2, y2);

    addQuad_numbers(x2, y2, side);
    addQuad_numbers(x1, y1, side);
    for (LOAPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]--;
    if (game_state != GAME_CONTINUE)
      game_state = GAME_CONTINUE;
//    if (debug_quads)
//      quadCheck();
    return true;
  }

// Check the end game condition, return player, draw, or in progress
public int endGame() {
//        if (game_state != GAME_OVER) // This isn't being caught.
    //          throw new Error("internal error: referee unfinished game");
    if (connected(PLAYER_BLACK))
      return PLAYER_BLACK;
    if (connected(PLAYER_WHITE))
      return PLAYER_WHITE;
    return GAME_CONTINUE;
  }

  /**
   * Test to see if this players pieces are all connected.
   *
   * First calculate the Euler value for the game from the quad counts
   * if this is 1 then perform an exhaustive check:
   * First, find a piece of the players color then count the
   * number of pieces connected to this one. Then test to make sure that the
   * map made from visiting all pieces connected to the first one found are
   * all the pieces the player has. If all of the pieces are accounted for
   * the player is connected.
   *
   * @param side int the player [PLAYER_WHITE, PLAYER_BLACK]
   * @return boolean true if all pieces connected, false if not.
   */
  boolean connected(int side) {
    int euler;

    // Before doing the full check just check the Euler number for this side.
    euler = (quadcount[side][1]-quadcount[side][3]-2*quadcount[side][5])/4;
    if ( euler > 1 )
      return false;

    LOAPiece plist = null;
    int count = 0;
    for (LOAPiece p = piece_list[side]; p != null; p = p.next) {
      plist =  new LOAPiece(p.x, p.y, side, plist, null);
      count++;
    }
    LOAPiece Connectlist = plist;
    plist = plist.next;
    Connectlist.next = null;
    boolean connection = true;
    while (count > 1 && plist != null && connection) {
      connection = false;
      LOAPiece p = plist;
      for (LOAPiece plist2 = Connectlist; plist2 != null && !connection;
           plist2 = plist2.next) {
        if ((p.x == plist2.x + 1 || p.x == plist2.x - 1
            || p.x == plist2.x)
            && (p.y == plist2.y || p.y == plist2.y + 1
            || p.y == plist2.y - 1)) {
          plist = plist.next;
          p.next = Connectlist;
          Connectlist = p;
          connection = true;
        }
      }
      if (!connection) {
        LOAPiece tracker = plist;
        for (p = plist.next; p != null && !connection; p = p.next) {
          for (LOAPiece plist2 = Connectlist; plist2 != null && !connection;
               plist2 = plist2.next) {
            if ((p.x == plist2.x + 1 || p.x == plist2.x - 1
                || p.x == plist2.x)
                && (p.y == plist2.y || p.y == plist2.y + 1
                || p.y == plist2.y - 1)) {
              tracker.next = p.next;
              p.next = Connectlist;
              Connectlist = p;
              connection = true;
            }
          }
          if (!connection)
            tracker = p;
        }
      }
    }
    return connection;
  }

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
  public double heuristicEvaluation(){
    return staticBoardEvaluator.heuristicEvaluation(this);
  }

  /**
   * This method generates the set of features for the current board state.
   * It is used during training, and in the heuristic evaluation of the state.
   *
   * Unlike in a 'traditional' heuristic evaluator, where it is self-opponent,
   * this method is always calculating black-white. This ensures that the
   * training is always from the same player perspective.
   *
   * @return double [] The board state feature vector
   */
  public double [] encodeBoardState() {
    int weightIndex = 0;
    double [] result = new double [numWeights];

    double construct = 0.0;
    double constructOpponent = 0.0;
    int xSum = 0;
    int ySum = 0;
    int xSumOpponent = 0;
    int ySumOpponent = 0;
    int outside = 0;
    int outsideOpponent = 0;
    int count = 0;
    int countOpponent = 0;
    double xCOM, yCOM, xCOMOpponent, yCOMOpponent;

    //  Connected feature
    if (connected(LOABoard.PLAYER_BLACK) )
      result[weightIndex] = 1.0;
    if (connected(LOABoard.PLAYER_WHITE))
      result[weightIndex] = -1.0;
    weightIndex++;

    ////////////////////////////////
    // The weights are as expected with SSE varying wildly. So attempting to normalize each feature
    /////////////////////////////////

    // Piece count feature, outside weight feature, and initial information for Center of Mass calculation
    for (LOAPiece p = piece_list[LOABoard.PLAYER_BLACK]; p != null; p = p.next) {
      xSum += p.x;
      ySum += p.y;
      if (p.x == 0 || p.x == BOARD_INDEX || p.y == 0 || p.y == BOARD_INDEX)
        outside++;
      count++;
    }
    for (LOAPiece p = piece_list[LOABoard.PLAYER_WHITE]; p != null; p = p.next) {
      xSumOpponent += p.x;
      ySumOpponent += p.y;
      if (p.x == 0 || p.x == BOARD_INDEX || p.y == 0 || p.y == BOARD_INDEX)
        outsideOpponent++;
      countOpponent++;
    }
    result[weightIndex++] = ((double)(count - countOpponent))/((double)((LOABoard.BOARD_SIZE-2)*2));
    result[weightIndex++] = ((double)(outside - outsideOpponent))/((double)((LOABoard.BOARD_SIZE-2)*2));

    // Determine Center of Mass
    xCOM = (double) xSum / (double) count;
    yCOM = (double) ySum / (double) count;
    xCOMOpponent = (double) xSumOpponent / (double) countOpponent;
    yCOMOpponent = (double) ySumOpponent / (double) countOpponent;

    // Average distance from CoM feature
    double AvgDistance = Avg_Distance(piece_list[LOABoard.PLAYER_BLACK], xCOM, yCOM);
    double AvgDistanceOpponent = Avg_Distance(piece_list[LOABoard.PLAYER_WHITE],
        xCOMOpponent, yCOMOpponent);
    result[weightIndex++] = (1.0/(AvgDistance+1.0) - 1.0/(AvgDistanceOpponent+1.0)); // /(BOARD_SIZE/2);

    // CoM distance from board center feature
    double centerValue = (CENTER_VALUE - Math.pow(MIDPOINT - xCOM, 2.0)
        - Math.pow(MIDPOINT - yCOM, 2.0));
    double centerValueOpponent = (CENTER_VALUE - Math.pow(MIDPOINT - xCOMOpponent, 2.0)
        - Math.pow(MIDPOINT - yCOMOpponent, 2.0));
    result[weightIndex++] = (centerValue - centerValueOpponent)/(LOABoard.BOARD_SIZE/2);

    // Piece density around CoM feature
    for (LOAPiece p = piece_list[LOABoard.PLAYER_BLACK]; p != null; p = p.next) {
      if ( (quad[LOABoard.PLAYER_BLACK][p.x + 1][p.y + 1] == 4
          || quad[LOABoard.PLAYER_BLACK][p.x + 1][p.y + 1] == 3)
          && Math.abs(xCOM - (0.5 + (double) p.x)) < 2.0
          && Math.abs(yCOM - (0.5 + (double) p.y)) < 2.0) {
        if (quad[LOABoard.PLAYER_BLACK][p.x + 1][p.y + 1] == 4)
          construct += 0.50;
        else
          construct += 0.25;
      }
    }
    for (LOAPiece p = piece_list[LOABoard.PLAYER_WHITE]; p != null; p = p.next) {
      if ( (quad[LOABoard.PLAYER_WHITE][p.x + 1][p.y + 1] == 4
          || quad[LOABoard.PLAYER_WHITE][p.x + 1][p.y + 1] == 3)
          && Math.abs(xCOMOpponent - (0.5 + (double) p.x)) < 2.0
          && Math.abs(yCOMOpponent - (0.5 + (double) p.y)) < 2.0) {
        if (quad[LOABoard.PLAYER_WHITE][p.x + 1][p.y + 1] == 4)
          constructOpponent += 0.50;
        else
          constructOpponent += 0.25;
      }
    }
    result[weightIndex++] = (construct - constructOpponent)/4.5;

/* *****************************************************************
 * For testing only going to use the primary 6 heuristic features
 * *****************************************************************
      // Mobility feature

      // Euler value feature

      // Board position features

      // Quad count features

      // Vertical, horizontal, and diagonal count features

      // Nonad features

      // Spin features
 */
    return result;
  } // end encodeBoardState


  /**
   * Calculates the average distance from the center of mass for each piece.
   *
   * @param list Piece
   * @param xCOM double
   * @param yCOM double
   * @return double
   */
  protected static double Avg_Distance(LOAPiece list, double xCOM, double yCOM) {
    double result = 1.0;
    int count = 0;

    for (LOAPiece p = list; p != null; p = p.next) {
      count++;
      double distance = 0.0;
      if ( Math.abs((double)p.x-xCOM) > Math.abs((double)p.y-yCOM) )
        distance = Math.abs( (double) p.x - xCOM) - 1.0;
      else
        distance = Math.abs( (double) p.y - yCOM) - 1.0;
      if (distance > 1.0)
        result += distance;
    }
    if (count > 9) // If there are a lot of pieces, ignore those possibly within one of the COM
      return (result - (double) (count - 9)) / (double) count;
    return result / (double) count;
  }

  /**
   * Assign a value to each move (Move.value) and return Util.QuickSort(move_list)
   *
   * Note that LOAMove has an LOAPiece that indicates the player for the move
   * @param move_list Moves to be rank ordered
   * @return sorted move_list
   */
  public Move moveOrdering(Move move_list, int depth){
    for (LOAMove m = (LOAMove)move_list; m != null; m = (LOAMove)m.next) {
      if (KillerMoves_Yes && m.equals(killer_moves[depth - 1]))
        m.value = 10000;
      if (HistoryMoves_Yes)
        m.value += history_moves[m.piece.owner][m.x1][m.y1][m.x2][m.y2];
      if (OutsideMoves_Yes &&
          (m.x2 == 0 || m.y2 == 0 || m.x2 == (BOARD_SIZE - 1) || m.y2 == (BOARD_SIZE - 1)))
        m.value -= 1;
    }

    return Util.QuickSort(move_list);
  }

  public void moveOrderingData(Move best_move, int depth, boolean pruned) {
    LOAMove best = (LOAMove)best_move;

    history_moves[to_move][best.x1][best.y1][best.x2][best.y2] += 1 << depth;
    if (!pruned) {
      if (killer_moves[depth] == null || best.value > killer_moves[depth].value)
        killer_moves[depth] = best;
    }
  }

  /*
   * Get the current game state's player identifier
   */
  public int getCurrentPlayer(){
    return to_move;
  }

  /*
   * Get the current game state's opponent identifier
   */
  public int[] getPlayerList(){
    return new int[] {PLAYER_BLACK, PLAYER_WHITE};
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
    return square[x][y];
  }

  /**
   * Converts the board to a string for printing purposes
   *
   * @return a printable board
   */
  public String toString() {
    String result = new String();

    if (game_state != GAME_CONTINUE)
      result = result.concat("*: \n");
    else if (to_move == PLAYER_WHITE)
      result = result.concat("white: \n");
    else
      result = result.concat("black: \n");

    // Print the board state.
    for (int j = (BOARD_SIZE-1); j >= 0; --j) {
      for (int i = 0; i < BOARD_SIZE; i++)
        switch (square[i][j]) {
          case EMPTY_SQUARE:  result = result.concat("."); break;
          case PLAYER_BLACK:  result = result.concat("b"); break;
          case PLAYER_WHITE:  result = result.concat("w"); break;
          default: result = result.concat("?");
        }
      result = result.concat("\r\n");
    }
    return result;
  }

  /**
   * Reads the board layout from a string with 'b' for black, 'w' for white
   * and 'X' for a blank into the board data structures.
   *
   * @param boardString String
   */
  public void loadBoard(String boardString) {
    char [] boardChars = boardString.toCharArray();
    System.out.println(boardChars);
    for ( int i = 0; i < BOARD_SIZE; i++ )
      for ( int j = 0; j < BOARD_SIZE; j++ ) {
        if ( boardChars[i*BOARD_SIZE+j] == 'b' )
          square[i][j] = PLAYER_BLACK;
        else if ( boardChars[i*BOARD_SIZE+j] == 'w' )
          square[i][j] = PLAYER_WHITE;
        else
          square[i][j] = EMPTY_SQUARE;
      }
    if (boardChars[BOARD_SIZE*BOARD_SIZE-1] == 'b')
      to_move = PLAYER_BLACK;
    else
      to_move = PLAYER_WHITE;

    refreshDataStructures();
  }

  /**
   * Used to create a dummy Move object of the correct type for the searches
   * @return a blank move
   */
  public  Move newMove(){
    return new LOAMove(0,0,0,0);
  }


  public void RecordHash(int depth, double value, int flag, Move best, TranspositionBoard brd) {
    transpositionTable.RecordHash(depth, value, flag, (LOAMove) best, (LOABoard)brd);
  }

  public int ProbeHash(int depth, double alpha, double beta) {
    return transpositionTable.ProbeHash(depth, alpha, beta);
  }

  public double ProbeValue(int d) {
    return transpositionTable.ProbeValue(d);
  }

  public Move ProbeMove() {
    return transpositionTable.ProbeMove();
  }

  public void clearTranspositionTable() { transpositionTable.clearTable(); }

  public boolean moveIsValid(Move move) {
    LOAMove m = (LOAMove)move;
    int dx = (m.x2 - m.x1) < 0 ? -1: 1;
    int dy = (m.y2 - m.y1) < 0 ? -1: 1;

    int d = dist(m.x1, m.y1, dx, dy);
    if ((m.x2 - m.x1) != d * dx) {
      if (debug_move_ok)
        System.out.println("leaving move_ok(): bad x disp");
      return false;
    }
    if ((m.y2 - m.y1) != d * dy) {
      if (debug_move_ok)
        System.out.println("leaving move_ok(): bad y disp");
      return false;
    }
    for (int q = 1; q < d; q++) {
      int xx = m.x1 + q * dx;
      int yy = m.y1 + q * dy;
      if (square[xx][yy] == opponent(square[m.x1][m.y1])) {
        if (debug_move_ok)
          System.out.println("leaving move_ok(): blocked");
        return false;
      }
    }
    if (square[m.x2][m.y2] == square[m.x1][m.y1]) {
      if (debug_move_ok)
        System.out.println("leaving move_ok(): self-capture");
      return false;
    }
    if (square[m.x1][m.y1] != to_move || square[m.x1][m.y1] == opponent(to_move) ) {
      if (debug_move_ok) {
        System.out.println("leaving move_ok(): not player's piece " +to_move+" "+square[m.x1][m.y1]+" ["+m.x1+", "+m.y1);
        throw new Error("Not my piece");}
      return false;
    }
    if (debug_move_ok)
      System.out.println("leaving move_ok(): success");
    return true;
  }

  /**
   * Calculates the distance a piece can move, by traversing the line through
   * the location [x,y] along line [dx,dy] and counting the pieces on the line.
   *
   * @param x int start column location
   * @param y int start row locations
   * @param dx int the rate of change in column location.
   * @param dy int the rate of change in row location.
   * @return int the distance that can be traveled along this line.
   */
  private int dist(int x, int y, int dx, int dy) {
    if (dx == 0 )
      return vertical_count[x];
    if ( dy == 0 )
      return horizontal_count[y];
    if ( dx+dy == 0 )
      return back_diag_count[x+y];
    return forward_diag_count[x+((BOARD_SIZE-1)-y)];
  }


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
   *  Performs the blocking test and generates the move data.
   *
   * @param result Move
   * @param row int
   * @param col int
   * @param x int
   * @param y int
   * @param pieces int
   * @param p Piece
   * @return Move
   */
  private LOAMove blocked(LOAMove result, int row, int col, int x, int y, int pieces , LOAPiece p) {
    for (int i = 1; i < pieces; i++) {
      if (square[row + i * x][col + i * y] == opponent(to_move))
        return result;
    }
    LOAMove m = new LOAMove(row, col, row + x * pieces, col + y * pieces, 0 ,p );
    m.next = result;
    result = m;
    moveCount++;
    return result;
  }

  /**
   * Finds the piece in the piece_list of player side in position x, y
   *
   * @param x int
   * @param y int
   * @param side int
   * @return Piece
   */
  private LOAPiece findPiece( int x, int y, int side ) {
    for( LOAPiece p = piece_list[side]; p != null; p = p.next ) {
      if ( p.x == x && p.y == y )
        return p;
    }
    return null;
  }

  /**
   * Outputs whether the checker in location [x,y] is owned by the player.
   *
   * @param player int The player to test [PLAYER_WHITE,PLAYER_BLACK]
   * @param x int The column value to test.
   * @param y int The row value to test.
   * @return final boolean True if player present
   */
  public final boolean checker_of( int player, int x, int y ) {
    if ( x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE )
      return false;
    if ( square[x][y] != player )
      return false;
    return true;
  }

  /**
   * addQuad adds a piece to the quad of player side in square x, y
   *
   * @param x int
   * @param y int
   * @param side int
   * @param number int
   * @return int
   */
  private int addQuad(int x, int y, int side, int number) {
    if (++number == 6)
      return 3;
    if (number == 2 && x >= 0 && x < (BOARD_SIZE-1) && y >= 0 && y < (BOARD_SIZE-1)
        && (square[x][y] == side && square[x + 1][y + 1] == side
        || square[x + 1][y] == side && square[x][y + 1] == side))
      return 5;
    return number;
  }

  /**
   * subtractQuad removes a piece from the quad of player side at x,y
   *
   * @param x int
   * @param y int
   * @param side int
   * @param number int
   * @return int
   */
  private int subtractQuad(int x, int y, int side, int number) {
    if (--number == 4)
      return 1;
    if (number == 2 && x >= 0 && x < (BOARD_SIZE-1) && y >= 0 && y < (BOARD_SIZE-1)
        && (square[x][y] == side && square[x + 1][y + 1] == side
        || square[x + 1][y] == side && square[x][y + 1] == side))
      return 5;
    return number;
  }

  /**
   * Assuming the board representation in square is correct, recount the quads,
   * regenerate the piece lists, and update the line counts
   */
  private void refreshDataStructures() {
    recountQuads();
    reloadPieceLists();
    for ( int i = 0; i < BOARD_SIZE; i++ ) {
      vertical_count[i] = horizontal_count[i] = forward_diag_count[i] = 0;
      back_diag_count[i] = forward_diag_count[i+(BOARD_SIZE-1)] = 0;
      back_diag_count[i+(BOARD_SIZE-1)] = 0;
    }
    for ( int i = 0; i < BOARD_SIZE; i++ )
      for ( int j = 0; j < BOARD_SIZE; j++ ){
        if (square[i][j] != EMPTY_SQUARE ) {
          vertical_count[i]++;
          horizontal_count[j]++;
          forward_diag_count[i + ((BOARD_SIZE-1) - j)]++;
          back_diag_count[i + j]++;
        }
      }
    System.out.println("DoneRefresh");
  }

  /**
   * Recounts and calibrates the quad values and quadcounts. Will be called by
   * reset layout and also will be used when loading a board from a file.
   */
  private void recountQuads() {
    for (int i = 0; i < 6; i++ )
      quadcount[0][i] = quadcount[1][i] = 0;
    for (int i = 0; i < BOARD_SIZE+1; i++) {
      for (int j = 0; j < BOARD_SIZE+1; j++) {
        quad[0][i][j] = quadValue(i, j, 0); // # of pieces in quad, 5 for diagonals per side
        quad[1][i][j] = quadValue(i, j, 1);
        quadcount[0][quad[0][i][j]]++; // quad counts per side
        quadcount[1][quad[1][i][j]]++;
      }
    }
  }

  /**
   * Helper function for recountQuads. For this quad, count the number of
   * pieces in the quad of color side. If the count = 2 and they are a
   * diagonal return 5 otherwise return the count.
   *
   * @param x integer value for quad x
   * @param y integer value for quad y
   * @param side integer of player to calculate (PLAYER_WHITE, PLAYER_BLACK)
   * @return integer count of pieces of color side in quad or 5 if diagonal
   */
  private int quadValue(int x, int y, int side) {
    int counter = 0;
    if ( checker_of(side,x-1,y-1) )
      counter++;
    if ( checker_of(side,x,y-1) )
      counter++;
    if ( checker_of(side,x-1,y) )
      counter++;
    if ( checker_of(side,x,y) )
      counter++;
    if (counter == 2 && ((checker_of(side,x,y) && checker_of(side,x-1,y-1))
        || (checker_of(side,x-1,y) && checker_of(side,x,y-1))))
      counter = 5;
    return counter;
  }

  /**
   * Uses the square to regenerate each player's piece list.
   */
  private void reloadPieceLists() {
    piece_list[0] = null;
    piece_list[1] = null;
    for ( int i = 0; i < BOARD_SIZE; i++ )
      for ( int j = 0; j < BOARD_SIZE; j++ ) {
        if ( square[i][j] == PLAYER_WHITE )
          piece_list[PLAYER_WHITE] = new LOAPiece(i,j,PLAYER_WHITE,piece_list[PLAYER_WHITE], null);
        if ( square[i][j] == PLAYER_BLACK )
          piece_list[PLAYER_BLACK] = new LOAPiece(i,j,PLAYER_BLACK,piece_list[PLAYER_BLACK], null);
      }
  }

  /**
   * Find and remove the piece at location x, y from the piece list 'list'
   *
   * @param list Piece
   * @param x int
   * @param y int
   * @param m Move
   * @return Piece
   */
  private LOAPiece deletePiece(LOAPiece list, int x, int y, LOAMove m) {
    if (list.x == x && list.y == y) {
      m.captured_piece = list;
      list.next.prev = null;
      return list.next;
    }
    LOAPiece old = list;
    for (LOAPiece p = list.next; p != null; p = p.next) {
      if (p.x == x && p.y == y) {
        m.captured_piece = p;
        old.next = p.next;
        if (p.next != null)
          p.next.prev = p.prev;
        return list;
      }
      old = p;
    }
    if ( m.captured_piece == null )
      System.out.println("Piece not found");
    return list;
  }

  /**
   * Insert piece p at the head of list 'list'
   *
   * @param list Piece
   * @param p Piece
   * @return Piece
   */
  private LOAPiece addPiece(LOAPiece list, LOAPiece p) {
    if (p.prev == null)
      list = p;
    else
      p.prev.next = p;
    if (p.next != null)
      p.next.prev = p;
//        p.next = list;
//        list = p;
    return list;
  }

  /**
   * Add one to line counts for position x,y
   *
   * @param x int
   * @param y int
   */
  private void addLines(int x, int y) {
    horizontal_count[y]++;
    vertical_count[x]++;
    forward_diag_count[x + ((BOARD_SIZE-1)-y)]++;
    back_diag_count[x + y]++;
  }

  /**
   * Subtract one from the line counts for position x,y
   *
   * @param x int
   * @param y int
   */
  private void subtractLines(int x, int y) {
    horizontal_count[y]--;
    vertical_count[x]--;
    forward_diag_count[x + ((BOARD_SIZE-1)-y)]--;
    back_diag_count[x + y]--;
  }

  // Quad Updating
  private void addQuad(int x, int y, int side) {
    quad[side][x + 1][y + 1] = addQuad(x, y, side, quad[side][x + 1][y + 1]);
    quad[side][x + 1][y] = addQuad(x, y - 1, side, quad[side][x + 1][y]);
    quad[side][x][y + 1] = addQuad(x - 1, y, side, quad[side][x][y + 1]);
    quad[side][x][y] = addQuad(x - 1, y - 1, side, quad[side][x][y]);
  }

  private void removeQuad(int x, int y, int side) {
    quad[side][x + 1][y + 1] = subtractQuad(x, y, side, quad[side][x + 1][y + 1]);
    quad[side][x + 1][y] = subtractQuad(x, y - 1, side, quad[side][x + 1][y]);
    quad[side][x][y + 1] = subtractQuad(x - 1, y, side, quad[side][x][y + 1]);
    quad[side][x][y] = subtractQuad(x - 1, y - 1, side, quad[side][x][y]);
  }

  // Quad counts updating
  private void addQuad_numbers(int x, int y, int side) {
    quadcount[side][quad[side][x + 1][y + 1]]++;
    quadcount[side][quad[side][x + 1][y]]++;
    quadcount[side][quad[side][x][y + 1]]++;
    quadcount[side][quad[side][x][y]]++;
  }

  private void subtractQuad_numbers(int x, int y, int side) {
    quadcount[side][quad[side][x + 1][y + 1]]--;
    quadcount[side][quad[side][x + 1][y]]--;
    quadcount[side][quad[side][x][y + 1]]--;
    quadcount[side][quad[side][x][y]]--;
  }

  private void quadCheck() {
    int tempquad[][][] = new int [2][BOARD_SIZE+1][BOARD_SIZE+1];
    int tempquadcount[][] = new int [2][6];

    for (int i = -1; i < BOARD_SIZE; i++) {
      for (int j = -1; j < BOARD_SIZE; j++) {
        tempquad[PLAYER_BLACK][i + 1][j + 1] = quadValue(i, j, PLAYER_BLACK); // # of pieces in quad, 5 for diagonals per side
        tempquad[PLAYER_WHITE][i + 1][j + 1] = quadValue(i, j, PLAYER_WHITE);
        tempquadcount[PLAYER_BLACK][tempquad[PLAYER_BLACK][i + 1][j + 1]]++; // quad counts per side
        tempquadcount[PLAYER_WHITE][tempquad[PLAYER_WHITE][i + 1][j + 1]]++;
      }
    }
    for (int i=0; i <= BOARD_SIZE; i++) {
      for (int j=0; j <= BOARD_SIZE; j++) {
        if (tempquad[PLAYER_BLACK][i][j] != quad[PLAYER_BLACK][i][j] || tempquad[PLAYER_WHITE][i][j] != quad[PLAYER_WHITE][i][j])
          System.out.println("QUADS DO NOT MATCH");
      }
    }
    for (int i = 0; i < 6; i++){
      if (tempquadcount[PLAYER_BLACK][i] != quadcount[PLAYER_BLACK][i] || tempquadcount[PLAYER_WHITE][i] != quadcount[PLAYER_WHITE][i])
        System.out.println("QUADCOUNTS DO NOT MATCH");
    }
  }
  
  
  public void printBoardRep()
  {
  }

}
