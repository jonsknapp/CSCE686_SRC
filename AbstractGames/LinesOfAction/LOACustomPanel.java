package AbstractGames.LinesOfAction;

import AbstractGames.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class LOACustomPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  Graphics offscreen;     // Declaration of offscreen buffer
  BufferedImage image;    // Image associated with the buffer
  private Image background, black_piece, white_piece; // The artwork

  public int user_move;   // State variable for user player move selection

  // Player move selection states
  public static final int NOT_MOVE = 1000;
  public static final int PICK_PIECE = 1001;
  public static final int PICK_MOVE = 1002;
  public static final int END_MOVE = 1003;
  public static final int WAIT1 = 1004;
  public static final int WAIT2 = 1005;

  LOABoard board; // The LOA board
  Search<LOABoard, LOAMove> search; // The Search algorithm
  public int x1, y1, x2, y2; // User move information

  private int depth;
  private LOAMove lastmove = null;
  private LOAGUI lgui;
  LOAWorker worker;
  private String player;

  private String computer;

  // Set SELF_PLAY to true to play by yourself (also, always select black when you start)
  private boolean SELF_PLAY = false;

  /**
   * Select the evaluator and search algorithm here
   */
  private void initializeGame() {
    board = new LOABoard(new BoardEvaluator("weights", 6)); // Initialize the board with the SBE
//    search = new MinimaxSearch<LOABoard, LOAMove>();
//    search = new MinimaxAlphaBetaSearch<LOABoard, LOAMove>();
    search = new MinimaxAlphaBetaTransposition<LOABoard, LOAMove>();
  }
  /**
   * The LOAWorker generates a thread that calls the WorkBoard bestMove
   * method.
   */
  final class LOAWorker extends SwingWorker<Integer, Void> {

    protected Integer doInBackground() throws Exception {
      long startTime;

      startTime = System.currentTimeMillis();
      System.out.println(getDepth());
      LOAMove move = search.findBestMove(board, getDepth());
      startTime = System.currentTimeMillis() - startTime;
      board.makeMove(move);
      lgui.statusTextArea.append(computer + " Move: " + move.toString()
          + "\n");
      lgui.statusTextArea.append("Time: " + (float) (startTime) / 1000.0
          + " s\n");
      System.out.println("Search Time: " + (startTime) / 1000.0
          + "s  Best move: " + move.toString() + "\n");
      lgui.status.setText("Your move as " + getPlayer() + ".");
      lastmove = move;

      return new Integer(board.endGame());
    }

    // Can safely update the GUI from this method.
    protected void done() {

      Integer result;
      try {
        // Retrieve the return value of doInBackground.
        result = get();
        if (result != LOABoard.GAME_CONTINUE) {
          if (board.endGame() == LOABoard.PLAYER_BLACK) {
            lgui.status.setText("GAME OVER Black wins!");
            lgui.statusTextArea.append("Black wins!");
          } else {
            lgui.status.setText("GAME OVER White wins!");
            lgui.statusTextArea.append("White wins!");
          }
        } else
          user_move = WAIT1;
        repaint();
      } catch (InterruptedException e) {
        // This is thrown if the thread is interrupted.
      } catch (ExecutionException e) {
        // This is thrown if we throw an exception
        // from doInBackground.
      }
    }
  }

  public LOACustomPanel() {

    setBorder(BorderFactory.createLineBorder(Color.black));

    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        handleMouse(e);
      }

      public void mouseClicked(MouseEvent e) {
        handleMouse(e);
      }
    });
    // Load the artwork
    loadImages();

    setDepth(3);
    initializeGame();
  }

  /**
   * Primary Constructor
   *
   * @param L a pointer to the GUI
   */
  public LOACustomPanel(LOAGUI L) {
    lgui = L;
    setBorder(BorderFactory.createLineBorder(Color.black));

    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        handleMouse(e);
        validate();
      }
    });
    // Load the artwork
    loadImages();

    setDepth(3);
    initializeGame();
  }

  protected void handleMouse(MouseEvent e) {
    int grid_x, grid_y;
    Boolean break_flag = false;
    LOAMove moves;
    int stat;
    while (user_move != NOT_MOVE || break_flag) {
      int x = e.getX();
      int y = e.getY();
      grid_x = (int) ((x - 8) / 35);
      grid_y = (7 - (int) ((y - 8) / 35));

      switch (user_move) {

        case WAIT1:
          // If the user did NOT click a piece return
          if (board.getPlayerAtLocation(grid_x, grid_y) == LOABoard.EMPTY_SQUARE) {
            return;
          }
          // If the user picked a piece identify it and fall through to
          // the PICK_PIECE block.
          x1 = grid_x;
          y1 = grid_y;
          user_move = PICK_PIECE;

        case PICK_PIECE:
          moves = board.generateMovesFromLocation(x1, y1);
          if (moves == null)
            user_move = WAIT1;
          else
            user_move = WAIT2;
          repaint();
          return;

        case WAIT2:
          if (board.getPlayerAtLocation(grid_x, grid_y) == board.getCurrentPlayer()) {
            x1 = grid_x;
            y1 = grid_y;
            x2 = y2 = -1;
            user_move = PICK_PIECE;
            break;
          }
          x2 = grid_x;
          y2 = grid_y;
          user_move = PICK_MOVE;

        case PICK_MOVE:
          moves = board.generateMovesFromLocation(x1, y1);
          while (moves != null) {
            if (moves.x2 == x2 && moves.y2 == y2) {
              // valid move, need to move piece and update screen.
              board.makeMove(moves);
              user_move = NOT_MOVE;
              lgui.statusTextArea.append(player + " Move: " + moves.toString()
                  + "\n");
              lgui.status.setText("Computer's move as " + computer + ".");
              lastmove = moves;
              if (board.endGame() != LOABoard.GAME_CONTINUE) {
                if (board.endGame() == LOABoard.PLAYER_BLACK) {
                  lgui.status.setText("GAME OVER Black wins!");
                  lgui.statusTextArea.append("Black wins!\n");
                } else {
                  lgui.status.setText("GAME OVER White wins!");
                  lgui.statusTextArea.append("White wins!\n");
                }
                repaint();
                return;
              }
              repaint();
              if (SELF_PLAY)
                user_move = NOT_MOVE;
              else {
                worker = new LOAWorker();
                worker.execute();
              }
              return;
            }
            moves = (LOAMove)moves.next;
          }
          if (board.getPlayerAtLocation(grid_x, grid_y) != board.EMPTY_SQUARE) {
            // they selected another piece
            user_move = PICK_PIECE;
            break;
          } else {
            // they must have clicked a random location
            return;
          }
        default:
          repaint();
      }
    }
    if (SELF_PLAY)
      user_move = WAIT1;
  }

  public void runWorkerExtern() {
    worker = new LOAWorker();
    worker.execute();
  }

  /**
   * Load the artwork and initialize the drawing surfaces
   */
  public void loadImages() {
    try {
      background = ImageIO.read(new File("LOA-Grid.png"));
      black_piece = ImageIO.read(new File("LOA-Black.png"));
      white_piece = ImageIO.read(new File("LOA-White.png"));
    } catch (IOException ex) {
      // handle exception...
    }
    // allocation of offscreen buffer
    image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
    offscreen = image.getGraphics();
    // initialize game state
    user_move = NOT_MOVE;
  }

  public Dimension getPreferredSize() {
    return new Dimension(300, 300);
  }

  /**
   * The overridden paint function, copies the background and all of the other
   * graphics bits to the background Graphic that will be updated when we call
   * canvas.repaint at the end.
   *
   * @param g
   *            Graphics the canvas to draw the board on.
   */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    // showStatus( status );

    // Copy the background image
    offscreen.drawImage(background, 0, 0, 300, 300, this);

    // If the computer moved previously show this move.
    // Doing this first so that when we draw the pieces, it overwrites part
    // of
    // line.
    if (lastmove != null) {
      offscreen.setColor(Color.yellow);
      offscreen.drawLine(lastmove.x1 * 35 + 25,
          (7 - lastmove.y1) * 35 + 25, lastmove.x2 * 35 + 25,
          (7 - lastmove.y2) * 35 + 25);
    }

    // Place each piece in the correct location.
    for (int x = 0; x < LOABoard.BOARD_SIZE; x++)
      for (int y = 0; y < LOABoard.BOARD_SIZE; y++) {
        if (board.getPlayerAtLocation(x, y) == LOABoard.PLAYER_BLACK)
          offscreen.drawImage(black_piece, (x) * 35 + 11,
              (7 - y) * 35 + 11, 30, 30, this);
        if (board.getPlayerAtLocation(x, y) == LOABoard.PLAYER_WHITE)
          offscreen.drawImage(white_piece, (x) * 35 + 11,
              (7 - y) * 35 + 11, 30, 30, this);
      }
    // If the player is moving, show them their possible moves.
    if (user_move == WAIT2) {
      LOAMove moves = board.generateMovesFromLocation(x1, y1);
      offscreen.setColor(Color.red);
      while (moves != null) {
        offscreen.drawLine(x1 * 35 + 25, (7 - y1) * 35 + 25,
            moves.x2 * 35 + 25, (7 - moves.y2) * 35 + 25);
        moves = (LOAMove)moves.next;
      }
    }

    g.drawImage(image, 0, 0, null);
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public String getPlayer() {
    return player;
  }

  public void setPlayer(String player) {
    this.player = player;
  }

  public String getComputer() {
    return computer;
  }

  public void setComputer(String computer) {
    this.computer = computer;
  }
}
