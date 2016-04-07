import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.Scanner;
//import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MazewarServerHandlerThread extends Thread {
	private Socket socket = null;

    private TreeMap<String, Point> loc = new TreeMap<String, Point>(); // location
    private static TreeMap<String, Point> loc1 = new TreeMap<String, Point>(); // location
    private TreeMap<String, Direction> dir = new TreeMap<String, Direction>(); // direction
    private static TreeMap<String, Direction> dir1 = new TreeMap<String, Direction>(); // direction

    public static Integer seqNum = 0; // sequence number
    public static ArrayList<ObjectOutputStream> toAllClient = new ArrayList<ObjectOutputStream>(); // socket output stream
    public static ConcurrentHashMap<Integer, MazePacket> serverQueue = new ConcurrentHashMap<Integer, MazePacket>(); // queue

    private final Lock _mutex = new ReentrantLock(true);

	public MazewarServerHandlerThread(Socket socket) {
		super("MazewarServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new MazewarServerHandlerThread to handle client");
	}

	public void run() {
        boolean gotByePacket = false;
		try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            MazePacket packetFromClient;
            
            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            while ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
                // register and broadcast
                if (packetFromClient.type == MazePacket.MAZE_REGISTER) {
                    dir.put(packetFromClient.name, packetFromClient.direction);
                    dir1.put(packetFromClient.name, packetFromClient.direction);
                    loc.put(packetFromClient.name, packetFromClient.location);
                    loc1.put(packetFromClient.name, packetFromClient.location);
                    toAllClient.add(toClient);

                    MazePacket packetToClient = new MazePacket();
                    packetToClient.type = MazePacket.MAZE_REPLY;

                    packetToClient.dir1 = dir1;
                    packetToClient.dir = dir;
                    packetToClient.loc1 = loc1;
                    packetToClient.loc = loc;

                    for (int i=0; i<toAllClient.size(); i++) {
                        toAllClient.get(i).writeObject(packetToClient);
                    }   
                }
                
                // broadcast respawn location and direction 
                if (packetFromClient.type == MazePacket.MAZE_RESPAWN) {
                    MazePacket packetToClient = new MazePacket();
                    packetToClient.type = MazePacket.MAZE_RESPAWN;
                    packetToClient.location = packetFromClient.location;
                    packetToClient.direction = packetFromClient.direction;
                    packetToClient.name = packetFromClient.name;
                    for (int i=0; i<toAllClient.size(); i++) {
                        toAllClient.get(i).writeObject(packetToClient);
                    } 
                }

                // enqueue
                else {
                    // Enforce atomic incrementing of sequence number
                    _mutex.lock();
                    seqNum++;
                    _mutex.unlock();
                    System.out.println("Sequence numer: " + seqNum);
                    serverQueue.put(seqNum, packetFromClient);        
                }
            } // close while
          
            fromClient.close();
            toClient.close();
			socket.close();
		} // close try

        catch (IOException e) {
			if(!gotByePacket) {
				e.printStackTrace();
            }
		} 
        catch (ClassNotFoundException e) {
            if(!gotByePacket)
                e.printStackTrace();
        }
	}
}