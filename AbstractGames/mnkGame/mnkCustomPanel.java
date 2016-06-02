package AbstractGames.mnkGame;

import AbstractGames.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class mnkCustomPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  Graphics offscreen;     // Declaration of offscreen buffer
  BufferedImage image;    // Image associated with the buffer
  private Image background, black_piece, white_piece; // The artwork

  public int user_move;   // State variable for user player move selection

  // Player move selection states
  public static final int NOT_MOVE = 1000;
  public static final int PICK_MOVE = 1002;

  mnkBoard board; // The mnk board
  Search<mnkBoard, mnkMove> search; // The Search algorithm
  public int x1, y1, x2, y2; // User move information

  private int depth;
  private mnkMove lastmove = null;
  private mnkGUI lgui;
  mnkWorker worker;
  private String player;

  private String computer;

  // Set SELF_PLAY to true to play by yourself (also, always select black when you start)
  private boolean SELF_PLAY = false;

  /**
   * Select the evaluator and search algorithm here
   */
  private void initializeGame() {
    board = new mnkBoard(4, 4, 4); // Initialize the board with the SBE
    search = new MinimaxSearch<mnkBoard, mnkMove>();
//    search = new MinimaxAlphaBetaSearch<mnkBoard, mnkMove>();
//    search = new MinimaxAlphaBetaTransposition<mnkBoard, mnkMove>();
  }
  /**
   * The mnkWorker generates a thread that calls the WorkBoard bestMove
   * method.
   */
  final class mnkWorker extends SwingWorker<Integer, Void> {

    protected Integer doInBackground() throws Exception {
      long startTime;

      startTime = System.currentTimeMillis();
      System.out.println(getDepth());
      mnkMove move = search.findBestMove(board, getDepth());
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
        if (result != mnkBoard.GAME_CONTINUE) {
          if (board.endGame() == mnkBoard.PLAYER_BLACK) {
            lgui.status.setText("GAME OVER Black wins!");
            lgui.statusTextArea.append("Black wins!");
            lgui.statusTextArea.append(board.toString());
          } else if (board.endGame() == mnkBoard.PLAYER_WHITE) {
            lgui.status.setText("GAME OVER White wins!");
            lgui.statusTextArea.append("White wins!");
            lgui.statusTextArea.append(board.toString());
          } else {
            lgui.status.setText("GAME OVER DRAW!");
            lgui.statusTextArea.append("Draw!\n");
          }
        } else
          user_move = PICK_MOVE;
        repaint();
      } catch (InterruptedException e) {
        // This is thrown if the thread is interrupted.
      } catch (ExecutionException e) {
        // This is thrown if we throw an exception
        // from doInBackground.
      }
    }
  }

  public mnkCustomPanel() {

    setBorder(BorderFactory.createLineBorder(Color.black));

    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        handleMouse(e);
      }

      public void mouseClicked(MouseEvent e) {
        handleMouse(e);
      }
    });
    // load the artwork
    loadImages();

    setDepth(3);
    initializeGame();
  }

  /**
   * Primary Constructor
   *
   * @param L a pointer to the GUI
   */
  public mnkCustomPanel(mnkGUI L) {
    lgui = L;
    setBorder(BorderFactory.createLineBorder(Color.black));

    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        handleMouse(e);
        validate();
      }
    });
    // load the artwork
    loadImages();

    setDepth(3);
    initializeGame();
  }

  protected void handleMouse(MouseEvent e) {
    int grid_x, grid_y;
    boolean break_flag = false;
    mnkMove m;

    while (user_move != NOT_MOVE || !break_flag) {
      int x = e.getX();
      int y = e.getY();
      grid_x = (int) ((x - 8) / 35);
      grid_y = (7 - (int) ((y - 8) / 35));

      if (user_move == PICK_MOVE) {
        // If the user did NOT click a blank square, return
        if (board.getPlayerAtLocation(grid_x, grid_y) != mnkBoard.EMPTY_SQUARE) {
          return;
        }
        // If the user picked a empty square,  place the player's mark
        m = new mnkMove(grid_x, grid_y, board.getCurrentPlayer());
        // valid move, need to move piece and update screen.
        board.makeMove(m);
        user_move = NOT_MOVE;
        lgui.statusTextArea.append(player + " Move: " + m.toString()
            + "\n");
        lgui.status.setText("Computer's move as " + computer + ".");
        lastmove = m;
        if (board.endGame() != mnkBoard.GAME_CONTINUE) {
          break_flag = true;
          if (board.endGame() == mnkBoard.PLAYER_BLACK) {
            lgui.status.setText("GAME OVER Black wins!");
            lgui.statusTextArea.append("Black wins!\n");
          } else if (board.endGame() == mnkBoard.PLAYER_WHITE) {
            lgui.status.setText("GAME OVER White wins!");
            lgui.statusTextArea.append("White wins!\n");
          } else {
            lgui.status.setText("GAME OVER DRAW!");
            lgui.statusTextArea.append("Draw!\n");
          }
          repaint();
          return;
        }
        repaint();
        if (SELF_PLAY)
          user_move = PICK_MOVE;
        else {
          worker = new mnkWorker();
          worker.execute();
        }
        return;
      }
    }
  }

  public void runWorkerExtern() {
    worker = new mnkWorker();
    worker.execute();
  }

  /**
   * load the artwork and initialize the drawing surfaces
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

    // Highlight the last move made.
    if (lastmove != null) {
      Graphics2D g2 = (Graphics2D) offscreen;
      g2.setStroke(new BasicStroke(2));
      g2.setColor(Color.magenta);
      g2.drawRect((lastmove.x*35+10), 300-((lastmove.y+1)*35+10), 33, 33);
    }

    // Place each piece in the correct location.
    for (int x = 0; x < board.getBoardX(); x++)
      for (int y = 0; y < board.getBoardY(); y++) {
        if (board.getPlayerAtLocation(x, y) == mnkBoard.PLAYER_BLACK)
          offscreen.drawImage(black_piece, (x) * 35 + 11,
              (7 - y) * 35 + 11, 30, 30, this);
        if (board.getPlayerAtLocation(x, y) == mnkBoard.PLAYER_WHITE)
          offscreen.drawImage(white_piece, (x) * 35 + 11,
              (7 - y) * 35 + 11, 30, 30, this);
      }
    // If the player is moving, show them their possible moves.
    if (user_move == PICK_MOVE) {
      // Nothing to do here.
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

