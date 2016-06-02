/**
 * --------------------------------------------------------------------------
 * Classification: UNCLASSIFIED
 * --------------------------------------------------------------------------
 *
 * Class: JSKRandomCSCE686
 * Program: Jon Knapp, implementation of random player for CSCE686
 *
 * DESCRIPTION: 
 * 
 * TITLE        JSKRandomCSCE686
 * DATE         20160601
 * VERSION      1.0
 * PROJECT      CSCE686 Class Project
 * AUTHOR       Jon Knapp
 * DESCRIPTION  This program implements the a random player
 * ALGORITHM    Random
 * OS           Mac OS
 * LANGUAGE     Java  
 * GLOBALS      
 *              BOARD ogBoard - Main Breakthrough board
 * PARAMETERS   
 *              int player - Current player
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
import AbstractGames.MersenneTwister;
import AbstractGames.Move;
import AbstractGames.Search;

public class JSKRandomCSCE686<BOARD extends Board, MOVE extends Move>
        implements Search<BOARD, MOVE>
{
  MersenneTwister ogRnd = null;

  public JSKRandomCSCE686(MersenneTwister rnd)
  {
    ogRnd = rnd;
  }
  
  @Override
  public MOVE findBestMove(BOARD board, int depth)
  {
    MOVE result = null;
    MOVE moves = (MOVE) board.generateMoves();

    ArrayList<Move> temp = new ArrayList<Move>();
    while (moves != null)
    {
      temp.add(moves);
      moves = (MOVE) moves.next;
    }
    int pos = (int) (Math.floor(ogRnd.nextDouble() * temp.size()));
    result = (MOVE) temp.get(pos);
    return result;
  } // findBestMove


} // JSKRandomCSCE686
