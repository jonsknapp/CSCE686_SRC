/**
 * --------------------------------------------------------------------------
 * Classification: UNCLASSIFIED
 * --------------------------------------------------------------------------
 *
 * Class: JSKMinimaxAlphaBetaCSCE686
 * Program: Jon Knapp, implementation of Minimax search for CSCE686
 *
 * DESCRIPTION: 
 * 
 * TITLE        JSKMinimaxAlphaBetaCSCE686
 * DATE         20160601
 * VERSION      1.0
 * PROJECT      CSCE686 Class Project
 * AUTHOR       Jon Knapp
 * DESCRIPTION  This program implements the Breakthrough search algorithm
 *              with two Nash equalibria, and solves two problems with PPAD complexity
 *              with an aggregate heuristic
 * ALGORITHM    Alpha Beta Minimax
 * OS           Mac OS
 * LANGUAGE     Java  
 * GLOBALS      
 *              BOARD ogBoard - Main Breakthrough board
                long ogTotalNodesSearched - total nodes searched
                long ogTotalNodesCulled - total times a node was abandoned
 * PARAMETERS   
 *              int player - Current player
 *              int depth - max depth for search
 * 

         --*********************************************************--
         --**      copyright (C)   2016                           **--
         --**                                                     **--
         --**      Permission to use, copy and distribute this    **--
         --**      software for educational purposes without      **--
         --**      fee is hereby granted provided that the        **--
         --**      copyright notice and this permission notice    **--
         --**      appear on all copies. Permission to modify     **--
         --**      the software is granted, but not the right     **--
         --**      to distribute the modified code. All           **--
         --**      modifications are to be distributed as         **--
         --**      changes to released versions by AFIT. Use      **--
         --**      of program should be explicity referenced.     **--
         --**                                                     **--
         --********************************************************
*/
package AbstractGames.Breakthrough.JSK;

import java.util.Random;

import AbstractGames.Board;
import AbstractGames.Move;
import AbstractGames.Search;
import AbstractGames.Breakthrough.BreakthroughBoard;

public class JSKMinimaxAlphaBetaCSCE686<BOARD extends Board, MOVE extends Move> implements Search<BOARD,MOVE>
{
  private BOARD ogBoard = null;
  
  private long ogTotalNodesSearched = 0;
  private long ogTotalNodesCulled = 0;
 
  private int h_value(int player, int depth)
  {
    int result = 0;
    result = (int)ogBoard.heuristicEvaluation();
    return result;
  } // h_value
  
  /*
   * This function takes a current move and evaluates the fitness of its descendant moves. The top level
   * of the miniMax is performed in the bestMove function, and it will select the best move at that level.
   * This function will first be called from bestMove as a min. It will generate the children moves and then
   * select the min/max at each successive level, depending upon whether or not the to_move is the owner,
   * or originating player of the miniMax algorithm.
   * 
   */
  private int miniMax(MOVE currentMove, int depth, int min, int max, int owner, int moves)
  {
    ogTotalNodesSearched++;
    if ((ogTotalNodesSearched % 10000000) == 0)
    {
      System.out.println("Test1: " + ogTotalNodesSearched + ", " + ogTotalNodesCulled);
    }

    // Check for max depth or winning condition
    if (depth <= 0 || ogBoard.endGame() != Board.GAME_CONTINUE)
    {
      // **** Solution/Feasibility ********************************************************************
      //return h_value(ogBoard.getCurrentPlayer(), depth);
      return h_value(owner, depth) - moves;
    }
    // Determine if the owner performs the move for max or the opponent for min
    if (ogBoard.getCurrentPlayer() == owner)
    {
      // **** Next State Generation ************************************************************************
      // Generate moves for current node
      MOVE child = (MOVE)ogBoard.generateMoves();
      int v = min;

      // Iterate through moves and check fitness for each
      while (child != null)
      {
        // Make potential move
        ogBoard.makeMove(child);
        
        // **** Selection ************************************************************************
        // Get fitness of child node
        int vP = miniMax(child, depth - 1, v, max, owner, moves + 1);
        
        // Determine if v should be updated. If vP is greater, then update.
        if (vP > v)
        {
          v = vP;
        }
        // Undo Move
        ogBoard.reverseMove(child);
        
        // **** Feasibility ************************************************************************
        if (v > max)
        {
          ogTotalNodesCulled++;
          return max;
        }
        
        // Advance to next child
        child = (MOVE)child.next;
      }
      return v;
    }
    else
    {
      int v = max;
      // **** Next State Generation ************************************************************************
      MOVE child = (MOVE)ogBoard.generateMoves();
      while (child != null)
      {
        // Iterate through moves and check fitness for each
        ogBoard.makeMove(child);
        
        // **** Selection ************************************************************************
        // Get fitness of child node
        int vP = miniMax(child, depth - 1, min, v, owner, moves + 1);
        if (vP < v)
        {
          v = vP;
        }
        // Undo Move
        ogBoard.reverseMove(child);

        // **** Feasibility ************************************************************************
        if (v < min)
        {
          ogTotalNodesCulled++;
          return min;
        }
        child = (MOVE)child.next;
      }
      return v;
    }   
  } // miniMax
  
  
  @Override
  public MOVE findBestMove(BOARD board, int depth)
  {
    ogBoard = board;
    
    ogTotalNodesSearched = 0;
    
    
    MOVE best_move = null;
    int runningNodeTotal = 0;
    int start_depth = 1;
    int i = 1;
    boolean stoptime = false;

    i = depth;
    //while (i <= depth && !stoptime)
    {
      ogTotalNodesSearched = 0;
      start_depth = i;

      // **** Initialization ********************************************************************
      // Make initial list of moves from current board
      MOVE child = (MOVE)ogBoard.generateMoves();
      int bestMove = Integer.MIN_VALUE;
      // Initialize alpha and beta
      int alpha = -JSKCSCE686Evaluator.INF;
      int beta = JSKCSCE686Evaluator.INF;
      
      ((BreakthroughBoard)ogBoard).owner = ogBoard.getCurrentPlayer();
      
      // At the max top level, it must evaluate all of its children before it knows if any of its children is optimal. I believe
      // this is the primary difference between what was presented and what I have implemented. However, because this algorithm
      // was presented as a valid implementation by multiple sources, and assuming the Max turn must evaluate all of its children
      // at least to depth 1, it appears as if this is a valid interpretation.
      
      // Set up variables to keep track of best moves
      best_move = child;
      int owner = ogBoard.getCurrentPlayer();
      int s = 0;
      while (child != null)
      {
        // We must scan the entire list. A depth of 1 will call the heuristic on all of the children. Otherwise, this level
        // operates as the first max level. The first min level will be handled by miniMax, and them max, min, max min... until 
        // the desired depth.
        
        // Make first level move
        ogBoard.makeMove(child);
        
        // Use minimax to determine viability of move
        alpha = Math.max(alpha, miniMax(child, start_depth - 1, alpha, beta, owner, 0));

        //ogBoard.printBoardRep();
        //System.out.println("Test2: " + s + ", " + alpha);
        child.value = alpha;
        // Test to see if the next max is the best move
        // **** Solution ********************************************************************
        if (alpha > bestMove)
        {
          // Set best move so far
          best_move = child;
          bestMove = alpha;
        }
        // Reverse hypothetical move
        ogBoard.reverseMove(child);
        child = (MOVE)child.next;
        s++;
      }
      
      // increment indexes: increase by two to avoid swapping between optimistic and pessimistic results
      i = i + 2;

    }
    //System.out.println("Test2: " + ogTotalNodesSearched);
    return best_move;
  } // findBestMove
  
  
  public long getTotalSearched()
  {
    return ogTotalNodesSearched;
  }

} // JSKMinimaxAlphaBetaCSCE686
