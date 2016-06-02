package AbstractGames;

/**
 * Each game should implement a StaticBoardEvaluator that returns an estimate
 * of the current game's winning potential. It is called from the board's
 * heuristicEvaluation() method.
 *
 * The double returned must be between [-1.0..1.0]. An easy way to scale is to
 * perform a tanh.
 */
public interface StaticBoardEvaluator {
  /**
   * Code for a game's heuristic evaluator.
   * @param board
   * @return a double value in [-1.0..1.0]
   */
  public double heuristicEvaluation(Board board);
}
