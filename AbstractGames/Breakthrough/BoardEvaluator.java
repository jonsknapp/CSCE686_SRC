package AbstractGames.Breakthrough;

import AbstractGames.Board;
import AbstractGames.MersenneTwister;
import AbstractGames.StaticBoardEvaluator;

import java.io.*;
import java.util.Scanner;

/**
 * Very close to a generic weighted function board evaluator.
 * To make it generic, I would have to include 'encodeBoardState' as a
 * method in the abstract Board class, and enforce a PLAYER_BLACK/PLAYER_WHITE
 * opponent definition limit in the same.
 *
 */
public class BoardEvaluator implements StaticBoardEvaluator {

  protected double [] weights;
  private final String FILENAME_EXT = ".wgt";
  private String weightFileName;
  protected int numberOfWeights;				// Number of weights in heuristic function


  /**
   * If we don't have preexisting weights, generate a random weight vector
   *
   * @param numWeights the number of evaluation functions
   */
  public BoardEvaluator(int numWeights) {
    numberOfWeights = numWeights;
    weights = new double[numberOfWeights];
    initializeWeightVector();
  }

  /**
   * If we have known weights, load the file.
   *
   * @param filename the file with the weights in it
   * @param numWeights the number of evaluation functions
   */
  public BoardEvaluator(String filename, int numWeights) {
    numberOfWeights = numWeights;
    weights = new double[numberOfWeights];
    try {
      loadWeights(filename);
    } catch (FileNotFoundException ioe) {
      System.out.println("Error: Could not load weights file " + weightFileName + FILENAME_EXT + " from disk. Creating new weights.");
//            initializeWeightVector();
    }
  }

  /**
   * A weighted function board evaluator.
   * Almost generic except for the call to the encodeBoardState and
   * access to PLAYER_WHITE.
   *
   * @param board
   * @return
   */
  public double heuristicEvaluation(Board board) {
    double result = 0.0;
    double [] features;

    // Calculate the feature values
    features = ((BreakthroughBoard)board).encodeBoardState();

    // Calculate the weighted heuristic
    for ( int i = 0; i < numberOfWeights; i++ ) {
      result += weights[i]*features[i];
    }

    if (board.getCurrentPlayer() == BreakthroughBoard.PLAYER_WHITE)
      result = -result;

    return result;
  }


  /**
   * Initialize the weight vector with new values.
   * Each weight will be a real in [-0.5..0.5]
   */
  private void initializeWeightVector() {
    MersenneTwister r = new MersenneTwister();
    double randomdouble;
    int randomInt;

    for (int i=0; i< numberOfWeights; i++) {
      randomdouble = r.nextDouble();
      randomInt = r.nextInt();
      //keep dividing by 0.5 until the number is between 0.0 and 0.5
      while (randomdouble > 0.5)
        randomdouble /= 2.0;

      //if the random int is even, multiply by -1 for a negative number
      if ((randomInt % 2) == 0)
        randomdouble *= -1.0;

      weights[i] = randomdouble;
      if (weights[i] == 0.0)
        i--;
    }//for
  }

  /**
   * Load the heuristic weights.
   *
   * File format is a series of doubles for each of the weights in ASCII text.
   *
   * @param fileName weight file name
   * @throws java.io.FileNotFoundException
   */
  public void loadWeights(String fileName) throws FileNotFoundException {
    weightFileName = fileName + FILENAME_EXT;

    Scanner scan;
    File file = new File(weightFileName);
    try {
      scan = new Scanner(file);

      weights = new double[numberOfWeights];
      for( int i = 0; i < numberOfWeights; i++ ) {
        if (!scan.hasNextDouble()) // out of weights in the file
          throw new FileNotFoundException();
        weights[i] =  scan.nextDouble();
        System.out.println(weights[i]);
      }
      scan.close();           // Close the stream
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }
  }//loadWeights

  /**
   * Save the heuristic weights.
   *
   * File format is a series of doubles for each of the weights in ASCII text.
   */
  public void saveWeights() {

    try {
      Writer writer = new OutputStreamWriter(new FileOutputStream(weightFileName));
      try {
        for( int i = 0; i < numberOfWeights; i++ )
          writer.write(Double.toString(weights[i]) + " ");
      } finally {
        writer.close();
      }//try
    }
    catch (IOException ioe) {
      System.out.println("Error: Unknown problem in saveWeights.");
    }
  } //saveWeights

}
