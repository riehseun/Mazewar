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

	public int type;
	public String name;
	public Integer num;
	public Integer numPlayers;
	public Point location;
	public Direction direction;
	public TreeMap<String, Point> loc;
	public TreeMap<String, Point> loc1;
	public TreeMap<String, Direction> dir;
	public TreeMap<String, Direction> dir1;
}