
/**
 * --------------------------------------------------------------------------
 * Classification: UNCLASSIFIED
 * --------------------------------------------------------------------------
 *
 * Class: JSKBreakthroughPlayer
 * Program: Jon Knapp, implementation of Breakthrough player for CSCE686
 *
 * DESCRIPTION: 
 * 
 * TITLE        JSKBreakthroughPlayer
 * DATE         20160601
 * VERSION      1.0
 * PROJECT      CSCE686 Class Project
 * AUTHOR       Jon Knapp
 * DESCRIPTION  This program implements a player function for Breakthrough
 * OS           Mac OS
 * LANGUAGE     Java  
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import AbstractGames.MersenneTwister;
import AbstractGames.Search;
import AbstractGames.Breakthrough.BreakthroughBoard;
import AbstractGames.Breakthrough.BreakthroughMove;
import AbstractGames.Breakthrough.BreakthroughPiece;

public class JSKBreakthroughPlayer
{
  public static final int PLAYER_DEPTH = 30;

  private BreakthroughBoard ogBoard = null;
  private int ogDepth = 20;
  private int ogBlackWins = 0;
  private int ogWhiteWins = 0;
  private int ogBlackPieces = 0;
  private int ogWhitePieces = 0;
  private int ogGameRuns = 1;
  private double ogBlackWinRatio = 0;
  private double ogWhiteWinRatio = 0;
  private int ogRunsPlayed = 0;
  boolean ogHalt = false;


  public JSKBreakthroughPlayer(int depth)
  {
    ogDepth = depth;
    initBoard();
  } // Breakthrough


  public void initBoard()
  {
    // Create Initial Board
    ogBoard = new BreakthroughBoard(new JSKCSCE686Evaluator());
  } // initBoard


  public void halt()
  {
    ogHalt = true;
  } // halt


  public void playGameSequence(int runs)
  {
    ogBlackWins = 0;
    ogWhiteWins = 0;
    ogRunsPlayed = 0;
    ogGameRuns = runs;
    MersenneTwister rnd = new MersenneTwister(1);

    //Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKRandomCSCE686<BreakthroughBoard, BreakthroughMove>(rnd);
    Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKMinimaxAlphaBetaCSCE686<BreakthroughBoard, BreakthroughMove>();
    //Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKSimulatedAnnealingCSCE686<BreakthroughBoard, BreakthroughMove>();

    Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKRandomCSCE686<BreakthroughBoard, BreakthroughMove>(rnd);
    //Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKMinimaxAlphaBetaCSCE686<BreakthroughBoard, BreakthroughMove>();
    //Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKSimulatedAnnealingCSCE686<BreakthroughBoard, BreakthroughMove>();

    int i = 0;
    long time = System.currentTimeMillis();
    while ((i < ogGameRuns) && (!ogHalt))
    {
      initBoard();
      playAIGame(whiteSearch, blackSearch, false, true, 9999);

      System.out.println("Game: " + i);
      i++;
    }
    ogBlackWinRatio = ogBlackWins / (ogRunsPlayed * 1.0);
    ogWhiteWinRatio = ogWhiteWins / (ogRunsPlayed * 1.0);
    System.out.println("");

    System.out.println("Black: " + (ogBlackWinRatio * 100) + "%");
    System.out.println("White: " + (ogWhiteWinRatio * 100) + "%");
    System.out.println(
            "Average Black Pieces: " + (ogBlackPieces / (ogGameRuns * 1.0)));
    System.out.println(
            "Average White Pieces: " + (ogWhitePieces / (ogGameRuns * 1.0)));
    System.out.println("Time: " + (System.currentTimeMillis() - time));

  } // playGameSequence
  
  
  public void testSpecificBoard(int runs)
  {
    ogBlackWins = 0;
    ogWhiteWins = 0;
    ogRunsPlayed = 0;
    ogGameRuns = runs;
    MersenneTwister rnd = new MersenneTwister(1);

    Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKRandomCSCE686<BreakthroughBoard, BreakthroughMove>(
            rnd);
    //Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKMinimaxAlphaBetaCSCE686<BreakthroughBoard, BreakthroughMove>();
    //Search<BreakthroughBoard, BreakthroughMove> whiteSearch = new JSKSimulatedAnnealingCSCE686<BreakthroughBoard, BreakthroughMove>();

    Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKRandomCSCE686<BreakthroughBoard, BreakthroughMove>(
            rnd);
    //Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKMinimaxAlphaBetaCSCE686<BreakthroughBoard, BreakthroughMove>();
    //Search<BreakthroughBoard, BreakthroughMove> blackSearch = new JSKSimulatedAnnealingCSCE686<BreakthroughBoard, BreakthroughMove>();

    int i = 0;
    long time = 0;
    long minTime = Long.MAX_VALUE;
    long maxTime = Long.MIN_VALUE;
    long minS = Long.MAX_VALUE;
    long maxS = Long.MIN_VALUE;
    long totalSearched = 0;
    
    while ((i < ogGameRuns) && (!ogHalt))
    {
      long time1 = System.currentTimeMillis();
      initBoard();
      whiteSearch = new JSKRandomCSCE686<BreakthroughBoard, BreakthroughMove>(rnd);
      playAIGame(whiteSearch, blackSearch, true, true, 7);

      whiteSearch = new JSKMinimaxAlphaBetaCSCE686<BreakthroughBoard, BreakthroughMove>();
      //whiteSearch = new JSKSimulatedAnnealingCSCE686<BreakthroughBoard, BreakthroughMove>();
      playAIGame(whiteSearch, blackSearch, true, true, 1);

      System.out.println("Game: " + i);
      time1 = System.currentTimeMillis() - time1;
      if (time1 < minTime)
      {
        minTime = time1;
      }
      if (time1 > maxTime)
      {
        maxTime = time1;
      }
      time = time + time1;
      long s = ((JSKMinimaxAlphaBetaCSCE686)whiteSearch).getTotalSearched();
      totalSearched += s;
      if (s < minS)
      {
        minS = s;
      }
      if (s > maxS)
      {
        maxS = s;
      }
      i++;
    }
    time = time / ogGameRuns;
    ogBlackWinRatio = ogBlackWins / (ogRunsPlayed * 1.0);
    ogWhiteWinRatio = ogWhiteWins / (ogRunsPlayed * 1.0);
    System.out.println("");

    System.out.println("Black: " + (ogBlackWinRatio * 100) + "%");
    System.out.println("White: " + (ogWhiteWinRatio * 100) + "%");
    System.out.println(
            "Average Black Pieces: " + (ogBlackPieces / (ogGameRuns * 1.0)));
    System.out.println(
            "Average White Pieces: " + (ogWhitePieces / (ogGameRuns * 1.0)));
    System.out.println("Time: " + time + ", " + minTime + ", " + maxTime);
    System.out.println("Nodes: " + totalSearched + ", " + minS + ", " + maxS);

  } // playGameSequence


  public void playAIGame(
          Search<BreakthroughBoard, BreakthroughMove> whiteSearch,
          Search<BreakthroughBoard, BreakthroughMove> blackSearch,
          boolean printGame, boolean recordState, int maxMoves)
  {
    int i = 0;
    Search<BreakthroughBoard, BreakthroughMove> search = null; // The Search algorithm
    boolean done = false;
    int winner = BreakthroughBoard.PLAYER_BLACK;

    while ((i < maxMoves) && (!done))
    {
      if (ogBoard.getCurrentPlayer() == BreakthroughBoard.PLAYER_WHITE)
      {
        search = whiteSearch;
      }
      else if (ogBoard.getCurrentPlayer() == BreakthroughBoard.PLAYER_BLACK)
      {
        search = blackSearch;
      }
      if (search != null)
      {
        BreakthroughMove m = search.findBestMove(ogBoard, ogDepth);
        ogBoard.makeMove(m);

        if (printGame)
        {
          ogBoard.printBoardRep();
        }
        int win = ogBoard.endGame();
        if (win != BreakthroughBoard.GAME_CONTINUE)
        {
          if (win == BreakthroughBoard.PLAYER_WHITE)
          {
            ogWhiteWins++;
          }
          if (win == BreakthroughBoard.PLAYER_BLACK)
          {
            ogBlackWins++;
          }
          done = true;

          // Count pieces
          for (int j = 0; j < ogBoard.square.length; j++)
          {
            for (int s = 0; s < ogBoard.square[j].length; s++)
            {
              if (ogBoard.square[j][s] == BreakthroughBoard.PLAYER_WHITE)
              {
                ogWhitePieces++;
              }
              else if (ogBoard.square[j][s] == BreakthroughBoard.PLAYER_BLACK)
              {
                ogBlackPieces++;
              }
            }

          }
        }
      }
      i++;
    }
    ogRunsPlayed++;
  } // playAIGame


  public static void printPieceList(BreakthroughPiece p)
  {
    int j = 0;
    while (p.next != null)
    {
      System.out.println("Piece " + j++ + ", " + p.toString());
      p = p.next;
    }
  } // printPieceList


  public static void printEncodedState(double[] m)
  {
    System.out.println("Board State ---------");
    for (int i = 0; i < m.length; i++)
    {
      System.out.println(i + ": " + m[i]);
    }
    System.out.println("Board State ---------");
  } // printEncodedState


  public static void main(String[] args) throws IOException
  {

    JSKBreakthroughPlayer b = new JSKBreakthroughPlayer(PLAYER_DEPTH);
    for (int i = 0; i < 1; i++)
    {
      //b.playGameSequence(100);
      b.testSpecificBoard(1);
    }

  }

} // Breakthrough
