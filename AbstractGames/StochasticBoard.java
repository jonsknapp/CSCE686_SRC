package AbstractGames;

/**
 *
 * Design choice: There are two ways to perform the stochastic search. One is for the board
 * to maintain all of the different player views and we overload all of the game checks
 * based upon the player's perspective.
 * The second is to have the global knowledge board, and a board for each individual.
 *
 * I have chosen the first option, because I think that in the end it will result in a smaller
 * memory footprint. In a dice game, there would be new boards created after each roll. In a card
 * game the new boards are only created at the beginning. But with an MCTS search the create/destroy
 * process would occur so often it would still expend memory.
 */
public abstract class StochasticBoard extends Board{

  public abstract int getEventCount(int perspective);

  public abstract StochasticEvent makeEvent(int perspective, int index);

  public abstract boolean reverseEvent(int perspective, int index, StochasticEvent e);

  public abstract double heuristicEvaluation(int perspective);

  public abstract int endGame(int perspective);

  public abstract Move generateMoves(int perspective);

  public abstract boolean makeMove(int perspective, Move m);

  public abstract boolean reverseMove(int perspective, Move m);
}
