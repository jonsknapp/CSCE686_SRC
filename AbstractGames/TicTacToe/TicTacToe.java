package AbstractGames.TicTacToe;

import java.io.IOException;
import AbstractGames.*;

/**
 *
 */

public class TicTacToe {

  public static void main(String[] args) throws IOException {
    TicTacToeBoard board = new TicTacToeBoard();
    board.loadBoard("X X O OOXX");

    MinimaxSearch<TicTacToeBoard,TicTacToeMove> search  = new MinimaxSearch<TicTacToeBoard,TicTacToeMove>();
    TicTacToeMove best = search.findBestMove(board, 9);

    System.out.println(best.toString());
    board.makeMove(best);
    System.out.println(board.toString());
  }
}
