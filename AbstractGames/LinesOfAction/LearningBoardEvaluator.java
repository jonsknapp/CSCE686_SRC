package AbstractGames.LinesOfAction;

/**
 * An almost generic reinforcement learning class for two player games.
 * To make generic: need to make BoardEvaluator full generic
 * and...
 */
public class LearningBoardEvaluator extends BoardEvaluator{

  //////////////////////////////
  //Begin RL stuff
  /////////////////////////////
  private double DISCOUNT_FACTOR = 0.7;
  private double LEARNING_RATE = 0.0005; // 0.05;
  private double [][] boardStates;
  private int stateCount;
  int numUpdateNodes;
  //////////////////////////////
  //End RL stuff
  /////////////////////////////

  /**
   * Constructor (calls base class)
   * @param numWeights
   */
  public LearningBoardEvaluator(int numWeights) {
    super(numWeights);
  }

  /**
   * Constructor (calls base class)
   * @param filename
   * @param numWeights
   */
  public LearningBoardEvaluator(String filename, int numWeights) {
    super(filename, numWeights);
  }

  /**
   * This method makes use of the boardStates, and depending on which player this is
   * and who won trains the weights with the results of the last game.
   *
   * @param numEpochs number of times to update based on the latest game.
   * @param player what side the learner is playing: PLAYER_BLACK/PLAYER_WHITE
   * @param winner the side that won the game: PLAYER_BLACK/PLAYER_WHITE
   */
  public void trainWeights(int numEpochs, int player, int winner) {
    // Will be using the boardStates member which has already encoded the
    // boards on the path to the endgame.
    // The board states must now be fit to a reinforcement learning curve.
    double[][] trainingOutput = new double[stateCount][1]; // this is the total output (useful if using an ANN).
    double nn_i_succ = 0.0;
    double nn_i = 0.0;

    //REALLY IMPORTANT: Training is from the perspective of the black player, so if black lost, training will
    //use negative values. If black won, it will be trained with positive values. This must be kept in mind
    //when querying the features for values!

    //every other value depends on this one.
    for (int i=stateCount-1; i>=0; i--) {
      //for convenience get the value of the nn at boardStates[i] and boardStates[i+1]
      if (i == stateCount-1) {
        if (winner == LOABoard.PLAYER_BLACK)
          nn_i_succ = 1;
        else if (winner == LOABoard.PLAYER_WHITE)
          nn_i_succ = -1;
        else // Draw
          nn_i_succ = 0;
      } else {
        nn_i_succ = DISCOUNT_FACTOR * nn_i;
      }

      nn_i = 0;
      for ( int j = 0; j < numberOfWeights; j++ )
        nn_i += boardStates[i][j] * weights[j];

      trainingOutput[i][0] = nn_i + LEARNING_RATE*(nn_i_succ-nn_i);

      for (int j=0; j< numberOfWeights; j++) {//each epoch involves training on every board state in the game
        weights[j] = weights[j] + LEARNING_RATE*(nn_i_succ-nn_i)*boardStates[i][j];
      }//for j
    }//for i

    ////begin debug
//	  System.out.println("Debug Message: Displaying array trainingOutput (after fit to TD learning curve) " + stateCount);
//	  for (int debug =0; debug < trainingOutput.length; debug++) {
//		System.out.println(trainingOutput[debug][0]);
//     }
    ////done debug

    System.out.println("Debug Message: Displaying weights array after update.");
    for (int i = 0; i < numberOfWeights; i++) {
      System.out.println(weights[i]);
    }

    System.out.println("Info Message: Done training weights on results from game.");
  }

  public void setDiscountFactor(double df) { DISCOUNT_FACTOR = df; }
  public void setLearningRate(double lr) {LEARNING_RATE = lr;}

}
