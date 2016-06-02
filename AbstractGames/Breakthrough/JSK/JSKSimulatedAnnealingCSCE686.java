/**
 * --------------------------------------------------------------------------
 * Classification: UNCLASSIFIED
 * --------------------------------------------------------------------------
 *
 * Class: JSKSimulatedAnnealingCSCE686
 * Program: Jon Knapp, implementation of simulated annealing/Monte Carlo search for CSCE686
 *
 * DESCRIPTION: 
 * 
 * TITLE        JSKSimulatedAnnealingCSCE686
 * DATE         20160601
 * VERSION      1.0
 * PROJECT      CSCE686 Class Project
 * AUTHOR       Jon Knapp
 * DESCRIPTION  This program implements the Breakthrough search algorithm
 *              with two Nash equalibria, and solves two problems with PPAD complexity
 *              with an aggregate heuristic using a hybrid simulated annealing/Monte Carlo search
 * ALGORITHM    Simulated Annealing/Monte Carlo
 * OS           Mac OS
 * LANGUAGE     Java  
 * GLOBALS      
                Random ogRnd - Random Number Generator
                int ogNumMoves - Number of moves for Monte Carlo Search
                int ogNumGames - Number of games for Monte Carlo Search
 * PARAMETERS   
 *              BOARD b - current board
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

import java.util.ArrayList;
import java.util.Random;

import AbstractGames.Board;
import AbstractGames.Move;
import AbstractGames.Search;

public class JSKSimulatedAnnealingCSCE686<BOARD extends Board, MOVE extends Move>
        implements Search<BOARD, MOVE>
{
  public static final int INF = 9999999;
  Random ogRnd = new Random();
  public int ogNumMoves = 7;
  public int ogNumGames = 10000;

  public double averageRandomGames(BOARD b, MOVE m, int numMoves, int numGames)
  {
    double result = 0;
    // In order to find a potential fitness for this initial move,
    // the algorithm will play a series of random games and 
    // average out the fitness at the end of a certain number of moves
    ArrayList<MOVE> moveArray = new ArrayList<MOVE>();
    int originalPlayer = b.getCurrentPlayer();

    // make initial move
    b.makeMove(m);
    //b.printBoardRep();

    int total = 0;
    double totalHeuristic = 0;

    if ((numGames % 2) != 0)
    {
      numGames = numGames + 1;
    }

    for (int i = 0; i < numGames; i++)
    {
      double localHeuristic = 0;
      boolean unexpectedTermination = false;
      moveArray.clear();
      // Play a random game
      boolean done = false;
      int j = 0;
      while ((j < numMoves) && (!done))
      {
        MOVE nextMove = null;
        MOVE moves = (MOVE) b.generateMoves();

        ArrayList<Move> temp = new ArrayList<Move>();
        while (moves != null)
        {
          temp.add(moves);
          moves = (MOVE) moves.next;
        }
        int pos = (int) (Math.floor(ogRnd.nextDouble() * temp.size()));
        nextMove = (MOVE) temp.get(pos);
        b.makeMove(nextMove);
        //b.printBoardRep();

        moveArray.add(nextMove);
        total++;
        if (b.endGame() != BOARD.GAME_CONTINUE)
        {
          unexpectedTermination = true;
          localHeuristic = b.heuristicEvaluation();
          done = true;
        }
        j++;
      }
      // Evaluate state of board after game
      // No matter who is going to move, evaluate according to original player.
      if (!unexpectedTermination)
      {
        localHeuristic = b.heuristicEvaluation();
      }
      // If the board is being evaluated for the enemy player, then inverse
      // the score. This can happen when the enemy wins or loses.
      if (b.getCurrentPlayer() != originalPlayer)
      {
        localHeuristic *= -1;
      }
      totalHeuristic += localHeuristic;

      // Now reverse all moves
      for (int s = moveArray.size() - 1; s >= 0; s--)
      {
        b.reverseMove(moveArray.get(s));
        //b.printBoardRep();
      }
    }

    b.reverseMove(m);
    //b.printBoardRep();

    result = (totalHeuristic / (total * 1.0));
    return result;
  } // averageRandomGames
  
  
  public int greedyGame(BOARD b, MOVE m, int numMoves)
  {
    int result = 0;
    // In order to find a potential fitness for this initial move,
    // the algorithm will play a series of random games and 
    // average out the fitness at the end of a certain number of moves
    ArrayList<MOVE> moveArray = new ArrayList<MOVE>();
    int originalPlayer = b.getCurrentPlayer();

    // make initial move
    b.makeMove(m);
    //b.printBoardRep();

    int total = 0;
    double localHeuristic = 0;
    moveArray.clear();
    boolean done = false;
    boolean unexpectedTermination = false;
    int j = 0;
    while ((j < numMoves) && (!done))
    {
      MOVE moves = (MOVE) b.generateMoves();
      int bestH = -INF;
      MOVE bestMove = null;

      while (moves != null)
      {
        b.makeMove(moves);
        int h = (int)b.heuristicEvaluation();
        if ((h > bestH) || (bestMove == null))
        {
          bestH = h;
          bestMove = moves;
        }
        b.reverseMove(moves);
        moves = (MOVE) moves.next;
      }
      
      moveArray.add(bestMove);
      b.makeMove(bestMove);
      //b.printBoardRep();

      if (b.endGame() != BOARD.GAME_CONTINUE)
      {
        unexpectedTermination = true;
        localHeuristic = b.heuristicEvaluation();
        done = true;
      }
      j++;
    }
    // Evaluate state of board after game
    // No matter who is going to move, evaluate according to original player.
    if (!unexpectedTermination)
    {
      localHeuristic = b.heuristicEvaluation();
    }
    // If the board is being evaluated for the enemy player, then inverse
    // the score. This can happen when the enemy wins or loses.
    if (b.getCurrentPlayer() != originalPlayer)
    {
      localHeuristic *= -1;
    }
    // Now reverse all moves
    for (int s = moveArray.size() - 1; s >= 0; s--)
    {
      b.reverseMove(moveArray.get(s));
      //b.printBoardRep();
    }

    // Reverse initial move
    b.reverseMove(m);
    //b.printBoardRep();

    result = (int) (localHeuristic / (total * 1.0));
    return result;
  } // greedyGame


  /**
   * Implements the simulated annealing algorithm
   */
  @Override
  public MOVE findBestMove(BOARD board, int depth)
  {
    MOVE result = null;

    // Get all current moves
    MOVE moves = (MOVE) board.generateMoves();
    MOVE currentMove = null;
    double currentH = -INF;
    double bestH = -INF;
    int currentPos = -1;
    MOVE bestMove = null;
    
    //board.printBoardRep();
    
    // **** Initialization ****
    // The algorithm starts by evaluation all of the moves at the first
    // level to get an estimate of where to start the search. This is
    // a greedy evaluation, because the fitness of the next move will not
    // necessarily mean that move is th ebest over several moves.
    
    // Select the current best move from the group
    ArrayList<Move> moveArray = new ArrayList<Move>();
    int p = 0;
    while (moves != null)
    {
      // Make initial first level move
      moveArray.add(moves);
      board.makeMove(moves);

      // Evaluate board
      int h = (int)board.heuristicEvaluation();
      if ((h > bestH) || (currentMove == null))
      {
        currentMove = moves;
        currentH = h;
        currentPos = p;
      }
      p++;
      board.reverseMove(moves);
      moves = (MOVE) moves.next;
    }

    // Perform simulated annealing to improve the possible solution.
    // Set initial temp
    double temp = 10000;

    // Cooling rate
    double coolingRate = 0.03;

    // Loop until system has cooled
    // Similar to backtracking, in that it will continue to search and update
    // the best move while the temperature is still above 1.
    while (temp > 1)
    {
      // Create a neighbor
      // There is not a clear definition of what a neighbor move would be for
      // breakthrough, so I will exploit a property of the algorithm that
      // creates moves by sequentially iterating through the piece list and
      // assigning moves. Any items sequentially close to the current node will
      // be a "neighbor".
      
      // **** Next State Generator ****
      // **** Feasibility ****
      int newPos = getNewPos(currentPos, moveArray.size());
      MOVE newMove = (MOVE) moveArray.get(newPos);
      
      // **** Selection ****
      // Play a series of random games to get an idea of the general fitness
      // of the move. If given infinite time, this function would return
      // the actual fitness of the move. In general, the more random games
      // that are player, the better it will be.
      double newH = averageRandomGames(board, newMove, ogNumMoves, ogNumGames);
      //System.out.println("Test1: " + newH);
      newMove.addH(newH);
      newH = (int)newMove.getAverageH();
      //int newH = greedyGame(board, newMove, 10);

      if (acceptanceProbability(currentH, newH, temp) > Math.random())
      {
        currentPos = newPos;
        currentH = newH;
        currentMove = newMove;
      }
      
      // **** Solution ****
      // Update solution if a better one has been found
      if (newH > bestH)
      {
        bestH = newH;
        bestMove = newMove;
      }
      System.out.println(temp + ", " + bestH);
      
      // Cool system
      temp *= 1-coolingRate;
    }

    result = bestMove;
    return result;
  } // findBestMove


  /**
   * Calculate the acceptance probability
   * 
   * @param energy
   * @param newEnergy
   * @param temperature
   * @return
   */
  public static double acceptanceProbability(double energy, double newEnergy,
          double temperature)
  {
    // If the new solution is better, accept it
    if (newEnergy < energy)
    {
      return 1.0;
    }
    // If the new solution is worse, calculate an acceptance probability
    return Math.exp((energy - newEnergy) / temperature);
  } // acceptanceProbability


  /**
   * Gets a new local move in the neighborhood of the current move.
   * 
   * **** Selection ****
   * 
   * @param pos
   * @param size
   * @return
   */
  private int getNewPos(int pos, int size)
  {
    int result = pos;
    double g = ogRnd.nextGaussian();
    double b = (g * (size / 4));
    pos = pos + (int)b;
    if (pos < 0)
    {
      pos = pos + size;
    }
    if (pos >= size)
    {
      pos = pos - size;
    }
    result = pos;
    return result;
  } // getNewPos

} // JSKSimulatedAnnealingCSCE686
