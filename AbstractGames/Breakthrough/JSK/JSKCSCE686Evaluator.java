/**
 * --------------------------------------------------------------------------
 * Classification: UNCLASSIFIED
 * --------------------------------------------------------------------------
 *
 * Class: JSKCSCE686Evaluator
 * Program: Jon Knapp, implementation of heuristics for CSCE686
 *
 * DESCRIPTION: 
 * 
 * TITLE        JSKCSCE686Evaluator
 * DATE         20160601
 * VERSION      1.0
 * PROJECT      CSCE686 Class Project
 * AUTHOR       Jon Knapp
 * DESCRIPTION  This program implements heuristics for the class project. The 
 *              heuristics include a game winning function and a force protection
 *              function.
 * ALGORITHM    Zero sum game heuristics
 * OS           Mac OS
 * LANGUAGE     Java  
 * GLOBALS      
 *              int ogOwner - current player
 * PARAMETERS   
 *              BOARD b - Current board
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
import AbstractGames.StaticBoardEvaluator;
import AbstractGames.Breakthrough.BreakthroughBoard;
import AbstractGames.Breakthrough.BreakthroughPiece;

/**
 * Very close to a generic weighted function board evaluator.
 * To make it generic, I would have to include 'encodeBoardState' as a
 * method in the abstract Board class, and enforce a PLAYER_BLACK/PLAYER_WHITE
 * opponent definition limit in the same.
 *
 */
public class JSKCSCE686Evaluator implements StaticBoardEvaluator
{
  public static final int INF = 9999999;
  public static final double MAX_CASUALTIES = 0.75;

  int ogOwner = 0;


  public JSKCSCE686Evaluator()
  {

  } // JSKCSCE686Evaluator

  
  private int getPieceFitnessCSCE686(BreakthroughPiece p, int player,
          BreakthroughBoard b)
  {
    int result = 0;
    int posFromEnemyHome = ((player == BreakthroughBoard.PLAYER_BLACK)
            ? (BreakthroughBoard.BOARD_INDEX - p.y) : p.y);

    if (posFromEnemyHome == 1)
    {
      result += INF;
    }
    else
    {
      // fitness is related to how close the piece is to the enemy home row, so inverse distance for fitness
      result = BreakthroughBoard.BOARD_INDEX - posFromEnemyHome;
    }
    result *= 2;
    return result;
  } // getPieceFitness
  

  private int getPieceFitnessCSCE899(BreakthroughPiece p, int player,
          BreakthroughBoard b)
  { 
    int result = 0;
    int homeRowProtected = 0;
    int friendlyDistance = 0;
    int friendlyOnCommandRows = 0;

    // First, check to see if the pieces are on the home row on cd and fg. If
    // so, they should be worth more
    if (((player == BreakthroughBoard.PLAYER_BLACK) && ((p.x == 1) || (p.x == 2)
            || (p.x == BreakthroughBoard.BOARD_INDEX - 1) || (p.x == BreakthroughBoard.BOARD_INDEX - 2))
            && (p.y == 0))
            || ((player == BreakthroughBoard.PLAYER_WHITE)
                    && ((p.x == 1) || (p.x == 2) || (p.x == BreakthroughBoard.BOARD_INDEX - 1)
                            || (p.x == BreakthroughBoard.BOARD_INDEX - 2))
                    && (p.y == BreakthroughBoard.BOARD_INDEX)))
    {
      homeRowProtected = 5;
    }

    // Reduce importance of any piece on the edges
    double mult = 1;
    if ((p.x == 0) || (p.x == BreakthroughBoard.BOARD_INDEX))
    {
      mult = mult + 0.75;
    }
    friendlyDistance = ((player == BreakthroughBoard.PLAYER_BLACK) ? (BreakthroughBoard.BOARD_SIZE - p.y)
            : p.y);

    friendlyDistance = (int) (friendlyDistance * mult);

    // Award points for any pieces on the cd and fg rows
    if ((p.x == 1) || (p.x == 2) || (p.x == BreakthroughBoard.BOARD_INDEX - 1)
            || (p.x == BreakthroughBoard.BOARD_INDEX - 2))
    {
      friendlyOnCommandRows++;
    }

    result = friendlyDistance + friendlyOnCommandRows + homeRowProtected;
    
    return result;
  } // getPieceFitness


  private int getWinningHeuristic(Board board)
  {
    int result = 0;

    BreakthroughBoard b = (BreakthroughBoard) board;

    //int to_move = b.getCurrentPlayer();
    int to_move = ogOwner;
    int opponent = b.opponent(to_move);
    int friendlyPieces = 0;
    int enemyPieces = 0;

    int bestFitness = Integer.MIN_VALUE;
    // Get total fitness for the friendly side
    for (BreakthroughPiece p = b.piece_list[to_move]; p != null; p = p.next)
    {
      /*int m = getPieceFitnessCSCE686(p, to_move, b);
      if (m > bestFitness)
      {
        bestFitness = m;
      }*/
      result += getPieceFitnessCSCE899(p, to_move, b);
      //System.out.println("Test2: " + result + ", " + getPieceFitnessCSCE686(p, to_move, b));
      friendlyPieces++;
    }
    //result += bestFitness;

    bestFitness = Integer.MIN_VALUE;
    // Get total fitness for the enemy side
    for (BreakthroughPiece p = b.piece_list[opponent]; p != null; p = p.next)
    {
      /*int m = getPieceFitnessCSCE686(p, opponent, b);
      if (m > bestFitness)
      {
        bestFitness = m;
      }*/
      result -= getPieceFitnessCSCE899(p, opponent, b);
      //System.out.println("Test3: " + result + ", " + getPieceFitnessCSCE686(p, opponent, b));
      enemyPieces++;
    }
    //result -= bestFitness;

    // This heuristic favors having more pieces than the other side.
    result += (friendlyPieces - enemyPieces) * 1;
    
    // If all friendly pieces are gone, the game is lost
    if (friendlyPieces == 0)
    {
      result = -INF;
    }
    // If all enemy pieces are gone, the game is won
    if (enemyPieces == 0)
    {
      result = INF;
    }
    
    int winner = b.endGame();
    if (winner == to_move)
    {
      result = INF;
    }
    else if (winner == opponent)
    {
      result = -INF;
    }

    return result;
  } // getWinningHeuristic


  private int getProtectionHeuristic(Board board)
  {
    int result = 0;
    
    BreakthroughBoard b = (BreakthroughBoard) board;

    //int to_move = b.getCurrentPlayer();
    int to_move = ogOwner;
    int opponent = b.opponent(to_move);
    int friendlyPieces = 0;
    int enemyPieces = 0;

    // In order to protect pieces, we will subtract a penalty for each piece lost
    int totalPieces = BreakthroughBoard.BOARD_SIZE * 2;

    // Get total number of pieces for the friendly side
    for (BreakthroughPiece p = b.piece_list[to_move]; p != null; p = p.next)
    {
      friendlyPieces++;
    }
    
    // Get total number of pieces for the friendly side
    for (BreakthroughPiece p = b.piece_list[opponent]; p != null; p = p.next)
    {
      enemyPieces++;
    }
    
    // Reward for having pieces
    result += (friendlyPieces) * 5;
    
    // Penalize for enemy having pieces
    result -= (enemyPieces) * 5;
    
    // We will have a "lose" condition for number of pieces. If the number of friendly pieces
    // falls below a certain threshold, we will consider that a losing game
    if ((friendlyPieces / (totalPieces * 1.0)) < MAX_CASUALTIES)
    {
      result = -1000;
    }
    
    if ((enemyPieces / (totalPieces * 1.0)) < MAX_CASUALTIES)
    {
      result = 1000;
    }
        
    return result;
  } // getProtectionHeuristic


  /**
   * Heuristics for Breakthrough
   *
   * @param board
   * @return
   */
  public double heuristicEvaluation(Board board)
  {
    double result = 0.0;

     ogOwner = ((BreakthroughBoard)board).owner;
    // **** Heuristic 1 ****
    // Get fitness for winning the game. There are two ways to win. Either move
    // a piece into the enemy's home row, or eliminate all of the enemy pieces

    result += getWinningHeuristic(board) + getProtectionHeuristic(board);
    //result += getWinningHeuristic(board);
    //System.out.println("f: " + getWinningHeuristic(board) + ", " + getProtectionHeuristic(board));

    //result = (int) ((ogRnd.nextDouble() * 2.0 - 1.0) * 10.0);

    return result;
  } // heuristicEvaluation

}
