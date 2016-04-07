import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

 /**
 * MazePacket
 * ============
 * 
 * Packet format of the packets exchanged between the Server and the Client
 * 
 */

public class MazePacket implements Serializable {

	/* define constants */
	public static final int MAZE_NULL = 0;

	public static final int MAZE_FORWARD = 101;
	public static final int MAZE_BACKUP = 102;
	public static final int MAZE_TURNLEFT = 103;
	public static final int MAZE_TURNRIGHT = 104;
	public static final int MAZE_FIRE = 105;

	public static final int MAZE_REGISTER = 201;
	public static final int MAZE_REPLY = 202;
	public static final int MAZE_TICK = 203;
	public static final int MAZE_RESPAWN = 204;

	public static final int MAZE_REQUEST = 205;
	public static final int MAZE_OK = 206;
	public static final int MAZE_BROADCAST = 207;

	public static final int MAZE_TICKLEADER = 208;
	//public static final int MAZE_POSITIONS = 209;
	//public static final int MAZE_POSITIONSREPLY = 210;
	
	public static final int MAZE_BROADCASTACK = 211;
	public static final int MAZE_REMOTE_SETUP = 212;
	public static final int MAZE_REMOTE_REPLY = 213;
	public static final int MAZE_REMOTE_REJECT = 214;

	public int type;
	public int move;
	public TreeMap<String, Integer> port;
	public TreeMap<String, Integer> port1;
	public TreeMap<String, String> host;
	public TreeMap<String, String> host1;

	public String hostt;
	public int portt;
	public Point location;
	public Direction direction;
	public int counter;

	public String name;
	public int lc;
	public int num;
	public int numPlayers;
	public int fireTick;
	
}