package AbstractGames.Breakthrough;

import AbstractGames.MersenneTwister;
import AbstractGames.Move;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Title:
 * Description:
 * This class implements a transposition table based upon a Zobrist Hash
 * for the game LOA. All functionality is encapsulated inside of this self-
 * contained class.
 */

public class Transposition {
    //constants to denote the player.
    public final int PLAYER_WHITE = 0;
    public final int PLAYER_BLACK = 1;

    public final long TABLE_SIZE = 1<<18; //Integer.MAX_VALUE;
    public final int UNKNOWN = -5;

    //long is 64 bit integer type. hash board is used to keep the random numbers used
    //for the zobrist hash. the first index represents the side. the second index represents the 64 square board.
    //any element of the board can be identified by f(x) = Board.BOARD_SIZE*y+x
    long to_move_hash_value; //just a constant. whenever black is to move, xor this into the hash value
    long[][] hashBoard = new long[2][BreakthroughBoard.BOARD_SIZE*BreakthroughBoard.BOARD_SIZE]; //range of 0..1, and 0..63

    //hashTable is the actual hash table where hash values are stored and retrieved
    HashElement[] hashTable = new HashElement[(int)TABLE_SIZE];

    public MersenneTwister randomNumberGenerator;
    long currentHashKey;
    public int matchkey = 0;

    private static boolean transposition_debug = false;

    /**
     * Transposition Table Constructor
     * Generates the random numbers for the Zobrist hash function.
     */
    public Transposition() {
      //only initialize the random number generator one time!
      randomNumberGenerator = new MersenneTwister();
        
      // Load the Transposition table values into array here.
  	  String inFileName = "zobrist"+new Integer(BreakthroughBoard.BOARD_SIZE).toString()+".hsh";
  	  try {
  		// Create necessary input streams
    	FileInputStream fis = new FileInputStream(inFileName); // Read from file
    	GZIPInputStream gzis = new GZIPInputStream(fis);     // Uncompress
  		ObjectInputStream in = new ObjectInputStream(gzis);  // Read objects
  		// Read in the table.
  		for ( int i = 0; i <=1; i++)
  		  for (int j = 0; j <= (BreakthroughBoard.BOARD_SIZE * BreakthroughBoard.BOARD_SIZE)-1;j++ ) {
  		    hashBoard[i][j] = ((Long)in.readObject()).longValue();
  		    if (hashBoard[i][j] == 0)
  		      hashBoard[0][0] = 0;
  		  }
  		to_move_hash_value = ((Long)in.readObject()).longValue();
  		in.close();                    // Close the stream and set it.
  	  }
  	  // Print out exceptions.  
  	  catch (IOException ioe) { 
  		System.out.println(ioe); 
        }//catch
  	  catch (ClassNotFoundException cnfe) { 
  		System.out.println(cnfe); 
        }//catch
  	    
  	  if (transposition_debug == true)
        System.out.println("-Zobrist Hash Keys-");
      if ( hashBoard[0][0] == 0 ) {
        for (int i=0; i <=1; i++)
          for (int j=0; j <= (BreakthroughBoard.BOARD_SIZE * BreakthroughBoard.BOARD_SIZE)-1; j++) {
            hashBoard[i][j] = Math.abs(randomNumberGenerator.nextLong());
            if (transposition_debug == true)
              System.out.println ( hashBoard[i][j] );
          }
        to_move_hash_value = Math.abs(randomNumberGenerator.nextLong());
    	String outFileName = "zobrist"+new Integer(BreakthroughBoard.BOARD_SIZE).toString()+".hsh";
    	try {
    	  FileOutputStream fos = new FileOutputStream(outFileName); 
    	  GZIPOutputStream gzos = new GZIPOutputStream (fos);     
    	  ObjectOutputStream out = new ObjectOutputStream(gzos); 
    															// Save objects
    	  for ( int i = 0; i <=1; i++)
    	    for (int j = 0; j <= (BreakthroughBoard.BOARD_SIZE * BreakthroughBoard.BOARD_SIZE)-1;j++ ) {
    	  	  out.writeObject(new Long(hashBoard[i][j]));
    	  	}
    	  out.writeObject(new Long(to_move_hash_value));
    	  out.flush();                            // Always flush the output.
    	  out.close();                            // And close the stream.
        } catch (IOException ioe) {
    	  System.out.println("Warning/Error: IO Exception while trying to save network state during periodic server backup...");
        }
      }
      else if (transposition_debug == true)
      	System.out.println(hashBoard[0][0]);
      
      //initialize currentHashKey for the empty board
      currentHashKey = 0;

      //put in white
      for (int i=1; i<= BreakthroughBoard.BOARD_SIZE-2; i++) {
        currentHashKey ^= hashBoard[PLAYER_WHITE][BreakthroughBoard.BOARD_SIZE*i];
        currentHashKey ^= hashBoard[PLAYER_WHITE][BreakthroughBoard.BOARD_SIZE*i+(BreakthroughBoard.BOARD_SIZE-1)];
      }

      //put in black
      for (int i=1; i <= BreakthroughBoard.BOARD_SIZE-2; i++) {
        currentHashKey ^= hashBoard[PLAYER_BLACK][i];
        currentHashKey ^= hashBoard[PLAYER_BLACK][i+BreakthroughBoard.BOARD_SIZE*(BreakthroughBoard.BOARD_SIZE-1)];
      }

      //the current player to move is black by default, so don't xor in a value
      //at this point, the value currentHashKey should represent the board in its starting position
      //and the hashValues needed for xor'ing new positions are all ready to go.
    } //constructor

    public void clearTable() {
    	for( int i = 0; i < TABLE_SIZE; i++ )
    	  hashTable[i] = null;
    }
    
    public void initialize(){
      //initialize currentHashKey for the empty board
      currentHashKey = 0;

      //put in white
      for (int i=1;  i< BreakthroughBoard.BOARD_SIZE-1; i++) {
        currentHashKey ^= hashBoard[PLAYER_WHITE][BreakthroughBoard.BOARD_SIZE*i];
        currentHashKey ^= hashBoard[PLAYER_WHITE][BreakthroughBoard.BOARD_SIZE*i+(BreakthroughBoard.BOARD_SIZE-1)];
      }

      //put in black
      for (int i=1; i < BreakthroughBoard.BOARD_SIZE-1; i++) {
        currentHashKey ^= hashBoard[PLAYER_BLACK][i];
        currentHashKey ^= hashBoard[PLAYER_BLACK][i+BreakthroughBoard.BOARD_SIZE*(BreakthroughBoard.BOARD_SIZE-1)];
      }
    }
    
    /**
     * makeMove
     * For every move/reversed that's made this must be called so that the value
     * of currentHashKey is updated. Also, this needs to account for captures
     * and for who is to move next (the person moving after this move was made).
     * Another nice thing about this hash is that when you need to reverse a
     * move, you just run this very same operation with the same parameters.
     *
     * @param   x1  x value of FROM square
     * @param   y1  y value of FROM square
     * @param   x2  x value of DEST square
     * @param   y2  y value of DEST square
     * @param   capture  TRUE if a capture takes place, FALSE otherwise
     * @param   colorMoved  PLAYER_WHITE or PLAYER_BLACK
     */
    public void makeMove(int x1, int y1, int x2, int y2, boolean capture, int colorMoved) {
        //hashValue(new) = hashValue(old) xor hashValue(square_from of piece moved) xor hashValue(square_to of piece moved)
        int opponentColorMoved;

        // A move has been made, XOR in the to_move_hash_value
        currentHashKey ^= to_move_hash_value;
        
        //account for white being 0 and not -1
        if (colorMoved == PLAYER_WHITE) {
            opponentColorMoved = PLAYER_BLACK; //opponent is still 1
        }
        else//player moved is black (1)
            opponentColorMoved = PLAYER_WHITE; //+1;

        //if a capture, also xor hashValue(square of piece captured)
         if (capture)
            currentHashKey = currentHashKey ^ hashBoard[colorMoved][BreakthroughBoard.BOARD_SIZE*y1+x1] ^
                             hashBoard[colorMoved][BreakthroughBoard.BOARD_SIZE*y2+x2] ^ hashBoard[opponentColorMoved][BreakthroughBoard.BOARD_SIZE*y2+x2];
        else
            currentHashKey = currentHashKey ^ hashBoard[colorMoved][BreakthroughBoard.BOARD_SIZE*y1+x1] ^
                             hashBoard[colorMoved][BreakthroughBoard.BOARD_SIZE*y2+x2];
    }

    /**
     * RecordHash
     * Generates a new board copying the Board b information.
     *
     * @param   depth Current search tree depth
     * @param   value current heuristic value
     * @param   flag  HASH_EXACT, HASH_ALPHA, or HASH_BETA
     * @param   best  Best move from this board
     */
    public void RecordHash(int depth, double value, int flag, BreakthroughMove best, BreakthroughBoard brd) {
        int index = getHashIndex();

        //for now, use 'always replace' scheme. all element to the table
        hashTable[index] = new HashElement(currentHashKey, depth, flag, value, best, brd);
    }

    /**
     * ProbeHash
     * Checks hashTable for the current board and returns the value if previously
     * visited.
     *
     * @param   depth Current search tree depth
     * @param   alpha current alpha value
     * @param   beta  current beta value
     * @return  HASH_EXACT, HASH_ALPHA, or HASH_BETA
     */
    public int ProbeHash(int depth, double alpha, double beta) {
      int index = getHashIndex();
      HashElement localHashElement = hashTable[index]; //get the hash element
      //if the hash element is null. i.e. the first time this probe is called, then we have
      //no good information to give, so return UNKNOWN
      if (localHashElement == null)
          return UNKNOWN;
      //otherwise:
      //make sure this element matches our key..it might not
      if(localHashElement.key == getHashKey()) {
        matchkey++;
        if (localHashElement.depth >= depth) {
          return localHashElement.flag;
        }//if localHashElement
      }
      //our hashtable didn't help us
      return UNKNOWN;
    }

    public double ProbeValue(int depth) {
      int index = getHashIndex();
      HashElement localHashElement = hashTable[index];
      if (localHashElement == null )
        return UNKNOWN;
      if ( localHashElement.depth >= depth )
        return localHashElement.value;
      return UNKNOWN;
    }

    public String ProbeBoard() {
        int index = getHashIndex();
        HashElement localHashElement = hashTable[index];
        if (localHashElement == null )
          return null;
        if (localHashElement.key != getHashKey())
        	return null;
        
        return localHashElement.b;
      }
    
    public Move ProbeMove() {
      int index = getHashIndex();
      HashElement localHashElement = hashTable[index];
      if (localHashElement == null )
        return null;

      if (localHashElement.key != getHashKey())
    	return null;
      BreakthroughMove m = new BreakthroughMove(localHashElement.mX1, localHashElement.mY1, localHashElement.mX2, localHashElement.mY2, (int)localHashElement.value, localHashElement.p);
      return m;
    }

    //the hash key this returns assumes the move in question has already been made, so it evaluates the board
    //as such
    private int getHashIndex() {
        return (int)(currentHashKey % TABLE_SIZE); //this is a safe cast. TABLE_SIZE has range of Integer.MAX_VALUE
    }

    public long getHashKey() {
    	return currentHashKey;
    }
    
    //inner class which defines the hash element. will be using it like a struct. i.e. no get/set methods
    private class HashElement {
        public long key;
        public int depth, flag;
        public double value;
        public int mX1, mX2, mY1, mY2;
        public BreakthroughPiece p;
        public String b;

        //each hash element stores the full 64 bit key, the depth to which this element was searched
        //a flag value representing if it was an exact, alpha, or beta cutoff, the board value here
        //and what the best move from this position was
        public HashElement(long key, int depth, int flag, double value, BreakthroughMove best, BreakthroughBoard brd) {
                this.key = key;
                this.depth = depth;
                this.flag = flag;
                this.value = value;
                if ( best != null ) {
                  this.mX1 = best.x1; //these shouldn't be publicly available in Move but are
                  this.mY1 = best.y1;
                  this.mX2 = best.x2;
                  this.mY2 = best.y2;
                  this.p = best.piece;
                }
                else
                  this.mX1 = this.mY1 = this.mX2 = this.mY2 = -1;
        }
    }//HashElement inner class
}//Transposition


