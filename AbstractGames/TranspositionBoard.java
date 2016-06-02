package AbstractGames;

/**
 *
 */
public abstract class TranspositionBoard extends Board {

  //constants for the hash elements
  public final int HASH_EXACT = 0;
  public final int HASH_ALPHA = 1;
  public final int HASH_BETA = 2;

  public abstract void RecordHash(int depth, double value, int flag, Move best, TranspositionBoard brd);

  public abstract int ProbeHash(int depth, double alpha, double beta);

  public abstract double ProbeValue(int d);

  public abstract Move ProbeMove();

  public abstract boolean moveIsValid(Move m);

  public abstract void clearTranspositionTable();

}
