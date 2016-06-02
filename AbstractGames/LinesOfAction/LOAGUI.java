package AbstractGames.LinesOfAction;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


public class LOAGUI extends JApplet {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public JLabel status;
  public JTextArea statusTextArea;
  private LOACustomPanel boardPanel;
  private boolean startAsBlack;

  public LOAGUI () {
  }

  public void init() {
    setSize(550,335);
    buildGUI();
  }

  public void buildGUI() {

    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[]{300, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
    gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE};
    gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
    setLayout(gridBagLayout);

    JLabel lblNewLabel = new JLabel("Depth");
    GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
    gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
    gbc_lblNewLabel.gridx = 9;
    gbc_lblNewLabel.gridy = 1;
    add(lblNewLabel, gbc_lblNewLabel);

    JLabel lblNewLabel_1 = new JLabel("Time");
    GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
    gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
    gbc_lblNewLabel_1.gridx = 10;
    gbc_lblNewLabel_1.gridy = 1;
    add(lblNewLabel_1, gbc_lblNewLabel_1);

    status = new JLabel("Current Status:",
        JLabel.CENTER);
    GridBagConstraints gbc_lblStatus = new GridBagConstraints();
    gbc_lblStatus.insets = new Insets(10, 10, 10, 10);
    gbc_lblStatus.gridheight = 1;
    gbc_lblStatus.gridwidth = 11;
    gbc_lblStatus.gridx = 0;
    gbc_lblStatus.gridy = 0;
    add(status, gbc_lblStatus);

    JComboBox<Integer> depthComboBox = new JComboBox<Integer>();
    depthComboBox.setModel(new DefaultComboBoxModel<Integer>(new Integer[] {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21}));
    depthComboBox.setSelectedIndex(1);
    depthComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        depthItemStateChanged(e);
      }
    });
    GridBagConstraints gbc_comboBox = new GridBagConstraints();
    gbc_comboBox.insets = new Insets(0, 0, 5, 5);
    gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
    gbc_comboBox.gridx = 9;
    gbc_comboBox.gridy = 2;
    add(depthComboBox, gbc_comboBox);

    JComboBox<Integer> timeComboBox = new JComboBox<Integer>();
    timeComboBox.setModel(new DefaultComboBoxModel<Integer>(new Integer[] {5, 10, 30, 60, 120, 240, 480}));
    timeComboBox.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        timeItemStateChanged(e);
      }
    });
    GridBagConstraints gbc_comboBox_1 = new GridBagConstraints();
    gbc_comboBox_1.insets = new Insets(0, 0, 5, 0);
    gbc_comboBox_1.fill = GridBagConstraints.HORIZONTAL;
    gbc_comboBox_1.gridx = 10;
    gbc_comboBox_1.gridy = 2;
    add(timeComboBox, gbc_comboBox_1);

    statusTextArea = new JTextArea();
    GridBagConstraints gbc_statusTextArea = new GridBagConstraints();
    gbc_statusTextArea.gridheight = 2;
    gbc_statusTextArea.gridwidth = 2;
    gbc_statusTextArea.fill = GridBagConstraints.BOTH;
    gbc_statusTextArea.gridx = 9;
    gbc_statusTextArea.gridy = 3;
    add(statusTextArea, gbc_statusTextArea);

    boardPanel = new LOACustomPanel(this);
    boardPanel.setSize(300,300);
    boardPanel.setDoubleBuffered(true);
    boardPanel.setBackground(Color.LIGHT_GRAY);
    GridBagConstraints gbc_boardPanel = new GridBagConstraints();
    gbc_boardPanel.gridheight = 3;
    gbc_boardPanel.gridwidth = 9;
    gbc_boardPanel.insets = new Insets(0, 0, 0, 5);
    gbc_boardPanel.fill = GridBagConstraints.BOTH;
    gbc_boardPanel.gridx = 0;
    gbc_boardPanel.gridy = 1;
    add(boardPanel, gbc_boardPanel);

    Object[] possibilities = {"Black", "White"};
    String s = (String)JOptionPane.showInputDialog(
        this,
        "Which side would you like to play?\n",
        "Customized Dialog",
        JOptionPane.PLAIN_MESSAGE,
        null,
        possibilities,
        "Black");

    //If a string was returned, say so.
    if (s.equals("White")) {
      // launch the SwingWorker thread for the computer to play black
      status.setText("Please select a search depth.");
      startAsBlack = true;
      boardPanel.setPlayer(new String("White"));
      boardPanel.setComputer(new String("Black"));
    } else {// else the player will play black and so we wait on interaction from them.
      startAsBlack = false;
      boardPanel.user_move = LOACustomPanel.WAIT1;
      status.setText("Your move as Black.");
      boardPanel.setPlayer(new String("Black"));
      boardPanel.setComputer(new String("White"));
    }
    validate();
  }

  void timeItemStateChanged(ItemEvent e) {
    String D = e.getItem().toString();
//	    int temp = Integer.parseInt(D);
//	    D = "Time changed to: " + temp + "s\n";
    D = "Time search restrictions are unavailable.\n";
    statusTextArea.append(D);
    status.setText(D);
//	    board.searchtime = temp*1000;
  }

  void depthItemStateChanged(ActionEvent e) {
    @SuppressWarnings("unchecked")
    JComboBox<Integer> cb = (JComboBox<Integer>)e.getSource();
    Integer d = (Integer)cb.getSelectedItem();
    String D = "Depth changed to: " + d + "\n";
    statusTextArea.append(D);
    status.setText(D);
    boardPanel.setDepth(Integer.valueOf(d));
    if (startAsBlack) {
      startAsBlack = false;
      boardPanel.runWorkerExtern();
    }
  }

  public static void main(String s[]) {
    JFrame frame = new JFrame("Lines of Action");
    frame.setBounds(100, 100, 600, 340);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        //    System.exit(0);
      }
    });
    LOAGUI id = new LOAGUI();
    id.buildGUI();
    frame.add("Center", id);
    frame.pack();
    frame.setVisible(true);
  }
}