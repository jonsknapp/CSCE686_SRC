package AbstractGames.Breakthrough;

import AbstractGames.*;

/**
 *
 */
public class BreakthroughBoard extends TranspositionBoard {

  public static final int BOARD_SIZE = 5;
  public static final int MAX_DEPTH = 250;

  public static final int EMPTY_SQUARE = -1;
  public static final int PLAYER_WHITE = 0;
  public static final int PLAYER_BLACK = 1;

  /**
   * The game board
   */
  public int square[][] = new int[BOARD_SIZE][BOARD_SIZE];

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
  public BreakthroughPiece piece_list[] = new BreakthroughPiece[2];

  private StaticBoardEvaluator staticBoardEvaluator;

  //////////////////////////////
  //Begin feature constants for static board evaluator
  /////////////////////////////
  private int numWeights = 6;
  public static final double MIDPOINT = 0.5*BOARD_SIZE-0.5; //1.5;
  public static final double CENTER_VALUE = MIDPOINT*MIDPOINT; //2.25;
  public static final int BOARD_INDEX = BOARD_SIZE-1;
  //////////////////////////////
  //End feature constants for static board evaluator
  /////////////////////////////

  private int game_state = GAME_CONTINUE;
  private int to_move = PLAYER_BLACK;
  public int owner = 0;

  /**
   * Board statistics
   */
  public int moveCount;

  /**
   * Data structures for move ordering.
   * killer_moves is the best move at each depth found so far
   * history_moves is the best move for every search performed rated by depth of search
   */
  BreakthroughMove killer_moves[] = new BreakthroughMove[MAX_DEPTH];
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
  public BreakthroughBoard(StaticBoardEvaluator h_value) {
    staticBoardEvaluator = h_value;
    initialize();
  }
 

  /**
   * Initializes the board, setting all pieces in the starting configuration.
   * This is called on Board creation as well as whenever the game restarts to
   * reset the entire board state.
   *
   */
  public void initialize()
  {
    to_move = PLAYER_BLACK;
    game_state = GAME_CONTINUE;
    piece_list[PLAYER_BLACK] = null;
    piece_list[PLAYER_WHITE] = null;
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        square[i][j] = EMPTY_SQUARE;
    for (int i = 0; i < BOARD_SIZE; i++)
    {
      square[i][0] = PLAYER_BLACK;
      square[i][1] = PLAYER_BLACK;
      piece_list[PLAYER_BLACK] = new BreakthroughPiece(i, 0, PLAYER_BLACK,
          piece_list[PLAYER_BLACK], null);
      piece_list[PLAYER_BLACK] = new BreakthroughPiece(i, 1, PLAYER_BLACK,
          piece_list[PLAYER_BLACK], null);
      square[i][BOARD_INDEX] = PLAYER_WHITE;
      square[i][BOARD_INDEX - 1] = PLAYER_WHITE;
      piece_list[PLAYER_WHITE] = new BreakthroughPiece(i, BOARD_INDEX, PLAYER_WHITE,
          piece_list[PLAYER_WHITE], null);
      piece_list[PLAYER_WHITE] = new BreakthroughPiece(i, BOARD_INDEX - 1, PLAYER_WHITE,
          piece_list[PLAYER_WHITE], null);
      vertical_count[i] = 4;
    }
    recountQuads();
    horizontal_count[0] = BOARD_SIZE;
    horizontal_count[1] = BOARD_SIZE;
    horizontal_count[BOARD_INDEX - 1] = BOARD_SIZE;
    horizontal_count[BOARD_INDEX] = BOARD_SIZE;

    // add diagonal counts, if useful for heuristics.
    // set transposition table to start position
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
  public Move generateMoves() {
    BreakthroughMove result = null;
    int direction = 1;
    if (to_move == PLAYER_WHITE)
      direction = -1;
    for (BreakthroughPiece p = piece_list[to_move]; p != null; p = p.next)
    {
      int i = p.x;
      int j = p.y;
      if (i < 0 || j < 0 || square[p.x][p.y] != to_move) // Sometimes the piece
        // list gets corrupted.
        // Skip the piece.
        continue;
      // There are only three possible moves for a piece;
      // Forward:
      if (square[p.x][p.y + direction] == EMPTY_SQUARE)
      {
        BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x, p.y + direction, 0, p);
        m.next = result;
        result = m;
        moveCount++;
      }
      // Diagonal Left:
      if ((direction == 1 && p.x > 0) || (direction == -1 && p.x < BOARD_INDEX))
      { // Check bounds: ignore top and bottom because of goal check
        if (square[p.x - direction][p.y + direction] != to_move)
        {
          BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x - direction, p.y + direction, 0, p);
          m.next = result;
          result = m;
          moveCount++;
        }
      }
      // Diagonal Right:
      if ((direction == 1 && p.x < BOARD_INDEX) || (direction == -1 && p.x > 0))
      { // Check bounds: ignore top and bottom because of goal check
        if (square[p.x + direction][p.y + direction] != to_move)
        {
          BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x + direction, p.y + direction, 0, p);
          m.next = result;
          result = m;
          moveCount++;
        }
      }
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
  public BreakthroughMove generateMovesFromLocation(int i, int j) {
    BreakthroughMove result = null;

    BreakthroughPiece p = findPiece(i, j, to_move);
    if (square[i][j] == to_move) {
      int direction = 1;
      if (to_move == PLAYER_WHITE)
        direction = -1;
      // Forward:
      if (square[p.x][p.y + direction] == EMPTY_SQUARE) {
        BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x, p.y + direction, 0, p);
        m.next = result;
        result = m;
      }
      // Diagonal Left:
      if ((direction == 1 && p.x > 0) || (direction == -1 && p.x < BOARD_INDEX)) { // Check bounds: ignore top and bottom because of goal check
        if (square[p.x - direction][p.y + direction] != to_move) {
          BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x - direction, p.y + direction, 0, p);
          m.next = result;
          result = m;
        }
      }
      // Diagonal Right:
      if ((direction == 1 && p.x < BOARD_INDEX) || (direction == -1 && p.x > 0)) { // Check bounds: ignore top and bottom because of goal check
        if (square[p.x + direction][p.y + direction] != to_move) {
          BreakthroughMove m = new BreakthroughMove(p.x, p.y, p.x + direction, p.y + direction, 0, p);
          m.next = result;
          result = m;
        }
      }
    }
    return result;
  }

  /**
   * Update the game state based upon the move submitted.
   *
   * @param move an BreakthroughMove
   * @return true if complete
   */
  public boolean makeMove(Move move) {
    BreakthroughMove m = (BreakthroughMove)move;
    // Check to see if this was a capture
    // boolean moveWasACapture = piece_of(opponent(to_move), m.x2, m.y2);
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
    BreakthroughPiece quad_list = null;
    if (Math.abs(x1 - x2) == 1 && (y1 - y2) == 0)
    {
      int max_row = Math.max(x1, x2);
      quad_list = new BreakthroughPiece(max_row, y1, side);
      quad_list.next = new BreakthroughPiece(max_row, y1 + 1, side);
    }
    else if ((x1 - x2) == 0 && Math.abs(y1 - y2) == 1)
    {
      int max_col = Math.max(y1, y2);
      quad_list = new BreakthroughPiece(x1, max_col, side);
      quad_list.next = new BreakthroughPiece(x1 + 1, max_col, side);
    }
    for (BreakthroughPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]++;

    // SIMPLE
    m.piece = findPiece(x1, y1, side);
    m.piece.x = m.x2;
    m.piece.y = m.y2;
    transpositionTable.makeMove(x1, y1, x2, y2, (m.captured != EMPTY_SQUARE),
        side);
    square[x2][y2] = square[x1][y1];
    square[x1][y1] = EMPTY_SQUARE;
    subtractLines(x1, y1);

    // Now update the quad information
    removeQuad(x1, y1, side);
    addQuad(x2, y2, side);
    // If this was a capture do the same for the opponent
    if (m.captured != EMPTY_SQUARE)
    {
      piece_list[m.captured] = deletePiece(piece_list[m.captured], x2, y2, m);
      subtractQuad_numbers(x2, y2, m.captured);
      removeQuad(x2, y2, m.captured);
      addQuad_numbers(x2, y2, m.captured);
    }
    else
      addLines(x2, y2);
    // Here is the recalculating of the quad counts
    addQuad_numbers(x2, y2, side);
    addQuad_numbers(x1, y1, side);
    for (BreakthroughPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]--;
    to_move = opponent(to_move);

    return true;
  }

  /**
   * Execute the removal of an action
   *
   * @param move Move the move to remove, should be the last move executed.
   */
  public boolean reverseMove(Move move) {
    BreakthroughMove m = (BreakthroughMove) move;
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
    transpositionTable.makeMove(x1, y1, x2, y2, (m.captured != EMPTY_SQUARE),
        side);
    addLines(m.x1, m.y1);
    // If we are only moving one square up, down or left, right need to ensure
    // that the quadcounts do not get off.
    BreakthroughPiece quad_list = null;
    if (Math.abs(x1 - x2) == 1 && (y1 - y2) == 0)
    {
      int max_row = Math.max(x1, x2);
      quad_list = new BreakthroughPiece(max_row, y1, side);
      quad_list.next = new BreakthroughPiece(max_row, y1 + 1, side);
    }
    else if ((x1 - x2) == 0 && Math.abs(y1 - y2) == 1)
    {
      int max_col = Math.max(y1, y2);
      quad_list = new BreakthroughPiece(x1, max_col, side);
      quad_list.next = new BreakthroughPiece(x1 + 1, max_col, side);
    }
    for (BreakthroughPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]++;

    // QUAD
    subtractQuad_numbers(x1, y1, side);
    subtractQuad_numbers(x2, y2, side);

    // Now update the quad information, will update through the methods
    // instead of expending memory
    removeQuad(x2, y2, side);
    addQuad(x1, y1, side);

    if (m.captured != EMPTY_SQUARE)
    {
      piece_list[m.captured] = addPiece(piece_list[m.captured],
          m.captured_piece);
      subtractQuad_numbers(x2, y2, m.captured);
      addQuad(x2, y2, m.captured);
      addQuad_numbers(x2, y2, m.captured);
    }
    else
      subtractLines(x2, y2);

    addQuad_numbers(x2, y2, side);
    addQuad_numbers(x1, y1, side);
    for (BreakthroughPiece p = quad_list; p != null; p = p.next)
      quadcount[side][quad[side][p.x][p.y]]--;
    if (game_state != GAME_CONTINUE)
      game_state = GAME_CONTINUE;

    return true;
  }

  /**
   * Check the end game condition, return player, draw, or in progress
   * @return
   */
  public int endGame() {
    if (winningCondition(PLAYER_BLACK))
      return PLAYER_BLACK;
    if (winningCondition(PLAYER_WHITE))
      return PLAYER_WHITE;
    return GAME_CONTINUE;
  }

  /**
   * Did this side get a piece across the board? Scan the goal row for a piece
   * for this side, if found return true: the player wins.
   *
   * @param side
   *          int the player [PLAYER_WHITE, PLAYER_BLACK]
   * @return boolean true if pieces crossed, false if not.
   */
  private boolean winningCondition(int side)
  {
    boolean result = false;

    int goal_row = 0;
    if (side == PLAYER_BLACK)
      goal_row = BOARD_INDEX;
    for (int i = 0; i < BOARD_SIZE; i++)
    {
      if (square[i][goal_row] == side)
      {
        result = true;
      }
    }

    if (piece_list[opponent(side)] == null)
    {
      result = true;
    }

    return result;
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
    if (winningCondition(PLAYER_BLACK) )
      result[weightIndex] = 1.0;
    if (winningCondition(PLAYER_WHITE))
      result[weightIndex] = -1.0;
    weightIndex++;

    ////////////////////////////////
    // The weights are as expected with SSE varying wildly. So attempting to normalize each feature
    /////////////////////////////////

    // Piece count feature, outside weight feature, and initial information for Center of Mass calculation
    for (BreakthroughPiece p = piece_list[PLAYER_BLACK]; p != null; p = p.next) {
      xSum += p.x;
      ySum += p.y;
      if (p.x == 0 || p.x == BOARD_INDEX || p.y == 0 || p.y == BOARD_INDEX)
        outside++;
      count++;
    }
    for (BreakthroughPiece p = piece_list[PLAYER_WHITE]; p != null; p = p.next) {
      xSumOpponent += p.x;
      ySumOpponent += p.y;
      if (p.x == 0 || p.x == BOARD_INDEX || p.y == 0 || p.y == BOARD_INDEX)
        outsideOpponent++;
      countOpponent++;
    }
    result[weightIndex++] = ((double)(count - countOpponent))/((double)((BOARD_SIZE-2)*2));
    result[weightIndex++] = ((double)(outside - outsideOpponent))/((double)((BOARD_SIZE-2)*2));

    // Determine Center of Mass
    xCOM = (double) xSum / (double) count;
    yCOM = (double) ySum / (double) count;
    xCOMOpponent = (double) xSumOpponent / (double) countOpponent;
    yCOMOpponent = (double) ySumOpponent / (double) countOpponent;

    // Average distance from CoM feature
    double AvgDistance = Avg_Distance(piece_list[PLAYER_BLACK], xCOM, yCOM);
    double AvgDistanceOpponent = Avg_Distance(piece_list[PLAYER_WHITE],
        xCOMOpponent, yCOMOpponent);
    result[weightIndex++] = (1.0/(AvgDistance+1.0) - 1.0/(AvgDistanceOpponent+1.0)); // /(BOARD_SIZE/2);

    // CoM distance from board center feature
    double centerValue = (CENTER_VALUE - Math.pow(MIDPOINT - xCOM, 2.0)
        - Math.pow(MIDPOINT - yCOM, 2.0));
    double centerValueOpponent = (CENTER_VALUE - Math.pow(MIDPOINT - xCOMOpponent, 2.0)
        - Math.pow(MIDPOINT - yCOMOpponent, 2.0));
    result[weightIndex++] = (centerValue - centerValueOpponent)/(BOARD_SIZE/2);

    // Piece density around CoM feature
    for (BreakthroughPiece p = piece_list[PLAYER_BLACK]; p != null; p = p.next) {
      if ( (quad[PLAYER_BLACK][p.x + 1][p.y + 1] == 4
          || quad[PLAYER_BLACK][p.x + 1][p.y + 1] == 3)
          && Math.abs(xCOM - (0.5 + (double) p.x)) < 2.0
          && Math.abs(yCOM - (0.5 + (double) p.y)) < 2.0) {
        if (quad[PLAYER_BLACK][p.x + 1][p.y + 1] == 4)
          construct += 0.50;
        else
          construct += 0.25;
      }
    }
    for (BreakthroughPiece p = piece_list[PLAYER_WHITE]; p != null; p = p.next) {
      if ( (quad[PLAYER_WHITE][p.x + 1][p.y + 1] == 4
          || quad[PLAYER_WHITE][p.x + 1][p.y + 1] == 3)
          && Math.abs(xCOMOpponent - (0.5 + (double) p.x)) < 2.0
          && Math.abs(yCOMOpponent - (0.5 + (double) p.y)) < 2.0) {
        if (quad[PLAYER_WHITE][p.x + 1][p.y + 1] == 4)
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
  protected static double Avg_Distance(BreakthroughPiece list, double xCOM, double yCOM) {
    double result = 1.0;
    int count = 0;

    for (BreakthroughPiece p = list; p != null; p = p.next) {
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
   * Note that BreakthroughMove has an BreakthroughPiece that indicates the player for the move
   * @param move_list Moves to be rank ordered
   * @return sorted move_list
   */
  public Move moveOrdering(Move move_list, int depth){
    for (BreakthroughMove m = (BreakthroughMove)move_list; m != null; m = (BreakthroughMove)m.next) {
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
    BreakthroughMove best = (BreakthroughMove)best_move;

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
    return new BreakthroughMove(0,0,0,0);
  }


  public void RecordHash(int depth, double value, int flag, Move best, TranspositionBoard brd) {
    transpositionTable.RecordHash(depth, value, flag, (BreakthroughMove) best, (BreakthroughBoard)brd);
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
    BreakthroughMove m = (BreakthroughMove)move;

    if (Math.abs(m.x2-m.x1) > 1) {
      if (debug_move_ok)
        System.out.println("leaving move_ok(): bad x disp");
      return false;
    }
    if (Math.abs(m.y2-m.y1) > 1) {
      if (debug_move_ok)
        System.out.println("leaving move_ok(): bad y disp");
      return false;
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
   * Outputs the opposite color of the player color sent in.
   *
   * @param player Integer Player's color.
   * @return Integer [PLAYER_WHITE,PLAYER_BLACK]
   */
  public final int opponent(int player) {
    if (player == PLAYER_WHITE)
      return PLAYER_BLACK;
    if (player == PLAYER_BLACK)
      return PLAYER_WHITE;
    throw new Error("internal error: bad player " + player);
  }

  /**
   * Finds the piece in the piece_list of player side in position x, y
   *
   * @param x int
   * @param y int
   * @param side int
   * @return Piece
   */
  private BreakthroughPiece findPiece( int x, int y, int side ) {
    for( BreakthroughPiece p = piece_list[side]; p != null; p = p.next ) {
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
   * Find and remove the piece at location x, y from the piece list 'list'
   *
   * @param list BreakthroughPiece
   * @param x int
   * @param y int
   * @param m BreakthroughMove
   * @return BreakthroughPiece
   */
  private BreakthroughPiece deletePiece(BreakthroughPiece list, int x, int y, BreakthroughMove m)
  {
    if (list.x == x && list.y == y)
    {
      m.captured_piece = list;
      if (list.next != null)
        list.next.prev = null;
      return list.next;
    }
    BreakthroughPiece old = list;
    for (BreakthroughPiece p = list.next; p != null; p = p.next)
    {
      if (p.x == x && p.y == y)
      {
        m.captured_piece = p;
        old.next = p.next;
        if (p.next != null)
          p.next.prev = p.prev;
        return list;
      }
      old = p;
    }
    if (m.captured_piece == null)
      System.out.println("Piece not found");
    return list;
  }


  /**
   * Insert piece p at the head of list 'list'
   *
   * @param list BreakthroughPiece
   * @param p BreakthroughPiece
   * @return BreakthroughPiece
   */
  private BreakthroughPiece addPiece(BreakthroughPiece list, BreakthroughPiece p)
  {
    if (p.prev == null)
      list = p;
    else
      p.prev.next = p;
    if (p.next != null)
      p.next.prev = p;
    // p.next = list;
    // list = p;
    return list;
  }

  /**
   * Add one to line counts for position x,y
   *
   * @param x int
   * @param y int
   */
  private void addLines(int x, int y)
  {
    horizontal_count[y]++;
    vertical_count[x]++;
    forward_diag_count[x + (BOARD_INDEX - y)]++;
    back_diag_count[x + y]++;
  }


  /**
   * Subtract one from the line counts for position x,y
   *
   * @param x int
   * @param y int
   */
  private void subtractLines(int x, int y)
  {
    horizontal_count[y]--;
    vertical_count[x]--;
    forward_diag_count[x + (BOARD_INDEX - y)]--;
    back_diag_count[x + y]--;
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
  private int addQuad(int x, int y, int side, int number)
  {
    if (++number == 6)
      return 3;
    if (number == 2 && x >= 0 && x < BOARD_INDEX && y >= 0 && y < BOARD_INDEX
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
  private int subtractQuad(int x, int y, int side, int number)
  {
    if (--number == 4)
      return 1;
    if (number == 2 && x >= 0 && x < BOARD_INDEX && y >= 0 && y < BOARD_INDEX
        && (square[x][y] == side && square[x + 1][y + 1] == side
        || square[x + 1][y] == side && square[x][y + 1] == side))
      return 5;
    return number;
  }


  // Quad Updating
  private void addQuad(int x, int y, int side)
  {
    quad[side][x + 1][y + 1] = addQuad(x, y, side, quad[side][x + 1][y + 1]);
    quad[side][x + 1][y] = addQuad(x, y - 1, side, quad[side][x + 1][y]);
    quad[side][x][y + 1] = addQuad(x - 1, y, side, quad[side][x][y + 1]);
    quad[side][x][y] = addQuad(x - 1, y - 1, side, quad[side][x][y]);
  }


  private void removeQuad(int x, int y, int side)
  {
    quad[side][x + 1][y + 1] = subtractQuad(x, y, side,
        quad[side][x + 1][y + 1]);
    quad[side][x + 1][y] = subtractQuad(x, y - 1, side, quad[side][x + 1][y]);
    quad[side][x][y + 1] = subtractQuad(x - 1, y, side, quad[side][x][y + 1]);
    quad[side][x][y] = subtractQuad(x - 1, y - 1, side, quad[side][x][y]);
  }


  // Quad counts updating
  private void addQuad_numbers(int x, int y, int side)
  {
    quadcount[side][quad[side][x + 1][y + 1]]++;
    quadcount[side][quad[side][x + 1][y]]++;
    quadcount[side][quad[side][x][y + 1]]++;
    quadcount[side][quad[side][x][y]]++;
  }


  private void subtractQuad_numbers(int x, int y, int side)
  {
    quadcount[side][quad[side][x + 1][y + 1]]--;
    quadcount[side][quad[side][x + 1][y]]--;
    quadcount[side][quad[side][x][y + 1]]--;
    quadcount[side][quad[side][x][y]]--;
  }

  /**
   * Assuming the board representation in square is correct, recount the quads,
   * regenerate the piece lists, and update the line counts
   */
  protected void refreshDataStructures()
  {
    recountQuads();
    reloadPieceLists();
    for (int i = 0; i < BOARD_SIZE; i++)
    {
      vertical_count[i] = horizontal_count[i] = forward_diag_count[i] = 0;
      back_diag_count[i] = forward_diag_count[i + BOARD_INDEX] = 0;
      back_diag_count[i + BOARD_INDEX] = 0;
    }
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
      {
        if (square[i][j] != EMPTY_SQUARE)
        {
          vertical_count[i]++;
          horizontal_count[j]++;
          forward_diag_count[i + (BOARD_INDEX - j)]++;
          back_diag_count[i + j]++;
        }
      }
    System.out.println("DoneRefresh");
  }


  /**
   * Recounts and calibrates the quad values and quadcounts. Will be called by
   * reset layout and also will be used when loading a board from a file.
   */
  private void recountQuads()
  {
    for (int i = 0; i < 6; i++)
      quadcount[0][i] = quadcount[1][i] = 0;
    for (int i = 0; i < BOARD_SIZE + 1; i++)
    {
      for (int j = 0; j < BOARD_SIZE + 1; j++)
      {
        quad[0][i][j] = quadValue(i, j, 0); // # of pieces in quad, 5 for
        // diagonals per side
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
          piece_list[PLAYER_WHITE] = new BreakthroughPiece(i,j,PLAYER_WHITE,piece_list[PLAYER_WHITE], null);
        if ( square[i][j] == PLAYER_BLACK )
          piece_list[PLAYER_BLACK] = new BreakthroughPiece(i,j,PLAYER_BLACK,piece_list[PLAYER_BLACK], null);
      }
  }
  
  
  public void printBoardRep()
  {
    for (int i = BOARD_SIZE - 1; i >= 0; i--)
    {
      System.out.print("--");
    }
    System.out.println();
    for (int i = BOARD_SIZE - 1; i >= 0; i--)
    {
      for (int j = 0; j < BOARD_SIZE; j++)
      {
        System.out.print(formatStringLength(getSquareRep((int)square[j][i]) + "", 2, " ", false));
      }
      System.out.println();
    }
    for (int i = BOARD_SIZE - 1; i >= 0; i--)
    {
      System.out.print("--");
    }
    System.out.println();
  } // printBoardRep
  
  /** Formats a String to a length, with prefix specified as a parameter
   * 
   * @param m
   *          - String to format
   * @param length
   *          - Lenght of String
   * @param theBuffer
   *          - String to append to front or back of the String
   * @return String - Formatted String */
  public static String formatStringLength(String m, int length, String theBuffer, boolean after)
  {
    StringBuffer result = new StringBuffer();

    if (m.length() == length)
    {
      result.append(m);
    }
    else if ((m.length() > length) && (length > 0))
    {
      if (after)
      {
        result.append(m.substring(0, length));
      }
      else
      {
        result.append(m.substring(m.length() - length, m.length()));
      }
    }
    else
    {
      if (after)
      {
        result.append(m);
      }
      for (int i = 0; i < (length - m.length()); i++)
      {
        result.append(theBuffer);
      }
      if (!after)
      {
        result.append(m);
      }
    }
    return result.toString();
  } // formatStringLength
  
  public String getSquareRep(int m)
  {
    String result = ".";
    if (m == PLAYER_WHITE)
    {
      result = "W";
    }
    else if (m == PLAYER_BLACK)
    {
      result = "B";
    }
    return result;
  } // getSquareRep

} // BreakthroughBoard
