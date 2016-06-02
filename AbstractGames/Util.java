package AbstractGames;

import java.math.BigInteger;

/**
 * A set of utility functions that can be used by multiple implementations, but
 * don't have a specific object association.
 */
public class Util {

  /**
   * Quicksort sorting by move value high to low, decreasing
   *
   * @param list Move
   * @return Move
   */
  public static Move QuickSort(Move list) {
    if (list == null || list.next == null)
      return list;
    Move smaller = null;
    Move p = list;
    list = list.next;
    p.next = null;
    Move bigger = p;
    while (list != null) {
      if (p.value < list.value) {
        Move temp = list;
        list = list.next;
        temp.next = bigger;
        bigger = temp;
      } else {
        Move temp = list;
        list = list.next;
        temp.next = smaller;
        smaller = temp;
      }
    }
    smaller = QuickSort(smaller);
    bigger = QuickSort(bigger);
    p.next = smaller;
    return bigger;
  }

  public static int factorial(int n) {
    int fact = 1; // this  will be the result
    for (int i = 1; i <= n; i++) {
      fact *= i;
    }
    return fact;
  }

  public static BigInteger combinationTotalBI(final int N, final int K) {
    BigInteger ret = BigInteger.ONE;
    for (int k = 0; k < K; k++) {
      ret = ret.multiply(BigInteger.valueOf(N-k))
          .divide(BigInteger.valueOf(k + 1));
    }
    return ret;
  }

  /**
   * Returns a combination total or nCr
   *
   * @param N the number of items to choose from (ordered!)
   * @param R the number of items being chosen
   * @return the number of combinations possible
   */
  public static long combinationTotal(final int N, final int R) {
    long ret = 1;

//    if (R == 0)
//      return 0;
    for (int r = 0; r < R; r++) {
      ret = ret * (N-r) / (r+1);
    }
    return ret;
  }

  /**
   *
   * combination(15, 7,2) would be the 15th item that would be return a 2 item
   * array that indicates which of the items in an ordered set we would use.
   * In this example, the return would be {3, 4} which would be the third item
   * followed by the item 4 after the third (seventh item) using English, it would
   * be C and G.
   *
   * @param index the index of the combination we want
   * @param N the number of items to choose from (ordered!)
   * @param K the number of items being chosen
   * @return
   */
  public static int[] combination(long index, int N, int K) {
    int count = 1;
    int n =N;
    int k = K-1;
    int []ret = new int[K];
    long combinations = combinationTotal(n-1,k);

    // If no stochastic event available then return.
    if (K == 0) {
      ret = new int[1];
      ret[0] = 0;
      return ret;
    }

    while (index - combinations > 0 && combinations != 0) {
      index = index-combinationTotal(--n,k);
      count++;
      combinations = combinationTotal(n-1,k);
    }
    ret[0] = count;
    int[] ret2;
    if (k > 1) {
      ret2 = combination(index, n-1,k);
      for (int i = 0; i < ret2.length; i++) {
        ret[1+i] = ret2[i];
      }
    } else if (ret.length == 2)
      ret[1] = (int)index;
//    else if (combinations == 0)
//      ret[0] = (int)index;

    return ret;
  }
}