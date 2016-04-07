import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Scanner;
import java.util.concurrent.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MazewarClientHandlerThread extends Thread {
    private static TreeMap<String, Direction> dir2 = new TreeMap<String, Direction>(); // direction
    private static TreeMap<String, Point> loc2 = new TreeMap<String, Point>(); // location
    private TreeMap<Integer, MazePacket> clientQueue = new TreeMap<Integer, MazePacket>(); // queue
    private HashMap<String, Client> remote = new HashMap<String, Client>(); // remote players
    private Queue<MazePacket> fireQueue = new LinkedList<MazePacket>();

    Socket socket = null;
    static ObjectOutputStream out = null; // only one instance of outputstream per client
    static ObjectInputStream in = null;

    static Client client = null; // only one local client 
    Maze maze = null;
    String name = "";
    String host = "";
    Integer port = 0;

	public MazewarClientHandlerThread(Client client, Maze maze, String name, String host, Integer port) {
		super("MazewarClientHandlerThread");
        this.client = client;
        this.maze = maze;
        this.name = name;
        this.host = host;
        this.port = port;
        try {
            socket = new Socket(host, port); 
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());       
        }
        catch (IOException e) {
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        }
		System.out.println("Created new MazewarClientHandlerThread to handle client");
	}

	public void run() {
        boolean gotByePacket = false;
        
		try {
            MazePacket packetFromServer;
            while ((packetFromServer = (MazePacket) in.readObject()) != null) {
                // If clients want to register
                if (packetFromServer.type == MazePacket.MAZE_REPLY) {

                    // treemap for direction
                    dir2 = packetFromServer.dir1;
                    if (!dir2.containsKey(packetFromServer.dir.firstEntry().getKey())) {
                        Direction value = packetFromServer.dir.firstEntry().getValue();
                        String key = packetFromServer.dir.firstEntry().getKey();
                        dir2.put(key, value);
                    }
                    //System.out.println("complete direction: " + dir2);

                    // treemap for location
                    loc2 = packetFromServer.loc1;
                    if (!loc2.containsKey(packetFromServer.loc.firstEntry().getKey())) {
                        Point value = packetFromServer.loc.firstEntry().getValue();
                        String key = packetFromServer.loc.firstEntry().getKey();
                        loc2.put(key, value);
                    }
                    //System.out.println("complete location: " + loc2);

                    Iterator t = loc2.entrySet().iterator();
                    while (t.hasNext()) {
                        Map.Entry pairx = (Map.Entry)t.next();
                        String x = (String)pairx.getKey();
                        Point location = (Point)pairx.getValue();
                        Direction direction = null;

                        Iterator u = dir2.entrySet().iterator();
                        while (u.hasNext()) {
                            Map.Entry pairy = (Map.Entry)u.next();
                            String y = (String)pairy.getKey();
                            if (x.equals(y)) {
                                direction = (Direction)pairy.getValue();
                            }
                        }

                        // check if client is not GUIClient or already added
                        if (!remote.containsKey(x) && !x.equals(client.getName())) {
                            Client remoteClient = new RemoteClient(x);
                            {
                                remote.put(x, remoteClient);
                                maze.addRemoteClient(remoteClient, location, direction); // add remote client
                                //System.out.println("now facing: " + remoteClient.getOrientation());
                            }    
                        }
                    }
                }

                else if (packetFromServer.type == MazePacket.MAZE_RESPAWN) {
                    MazePacket packetToClient = new MazePacket();
                    packetToClient.type = MazePacket.MAZE_RESPAWN;
                    packetToClient.location = packetFromClient.location;
                    packetToClient.direction = packetFromClient.direction;
                    packetToClient.name = packetFromClient.name;
                    for (int i=0; i<toAllClient.size(); i++) {
                        toAllClient.get(i).writeObject(packetToClient);
                    }
                    
                    if (remote.containsKey(packetFromServer.name)) {
                        Client me = remote.get(packetFromServer.name);
                        Direction d = packetFromServer.direction;
                        // Force constant direction when mazes respawn
                        while (!me.getOrientation().equals(d)) {
                            me.turnRight();
                        }     
                    }
                    else {
                        // Lock keys such that users don't interrupt while mazez fix their directions
                        ((GUIClient)client).keyLock = 0;
                    }
                }

                // If client moves, enqueue
                else {
                    clientQueue.put(packetFromServer.num, packetFromServer); 
                }

                // dequeue if queue is not empty
                if (clientQueue.isEmpty() == false) {
                    MazePacket packet = null;
                    Integer num = 0;
                    String name = "";
                    Iterator it = clientQueue.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pairs = (Map.Entry)it.next();
                        packet = (MazePacket)pairs.getValue();
                        name = packet.name;
                        num = (Integer)pairs.getKey();
                        
                        if (packetFromServer.type == MazePacket.MAZE_FORWARD) {
                            if (packetFromServer.name.equals(client.getName())) {
                                System.out.println("sequence number - " + num + ": client forward");
                                client.forward();
                            }
                            else {
                                System.out.println("sequence number - " + num + ": remote forward");
                                Iterator itt = remote.entrySet().iterator();
                                while (itt.hasNext()) {
                                    Map.Entry pairss = (Map.Entry)itt.next();
                                    String remoteUser = (String)pairss.getKey();
                                    if (packetFromServer.name == remoteUser) {
                                        ((Client)pairss.getValue()).forward();
                                    } 
                                }
                                
                            }
                        }
                        else if (packetFromServer.type == MazePacket.MAZE_BACKUP) {
                            if (packetFromServer.name.equals(client.getName())) {
                                System.out.println("sequence number - " + num + ": client backup");
                                client.backup();
                            }
                            else {
                                System.out.println("sequence number - " + num + ": remote backup");
                                Iterator itt = remote.entrySet().iterator();
                                while (itt.hasNext()) {
                                    Map.Entry pairss = (Map.Entry)itt.next();
                                    String remoteUser = (String)pairss.getKey();
                                    if (packetFromServer.name == remoteUser) {
                                        ((Client)pairss.getValue()).backup();
                                    } 
                                }
                            }
                        }
                        else if (packetFromServer.type == MazePacket.MAZE_TURNLEFT) {
                            if (packetFromServer.name.equals(client.getName())) {
                                System.out.println("sequence number - " + num + ": client left");
                                client.turnLeft();
                            }
                            else {
                                System.out.println("sequence number - " + num + ": remote left");    
                                Iterator itt = remote.entrySet().iterator();
                                while (itt.hasNext()) {
                                    Map.Entry pairss = (Map.Entry)itt.next();
                                    String remoteUser = (String)pairss.getKey();
                                    if (packetFromServer.name == remoteUser) {
                                        ((Client)pairss.getValue()).turnLeft();
                                    } 
                                }
                                
                            }
                        }
                        else if (packetFromServer.type == MazePacket.MAZE_TURNRIGHT) {
                            if (packetFromServer.name.equals(client.getName())) {
                                System.out.println("sequence number - " + num + ": client right");
                                client.turnRight();
                            }
                            else {
                                System.out.println("sequence number - " + num + ": remote right");
                                
                                Iterator itt = remote.entrySet().iterator();
                                while (itt.hasNext()) {
                                    Map.Entry pairss = (Map.Entry)itt.next();
                                    String remoteUser = (String)pairss.getKey();
                                    if (packetFromServer.name == remoteUser) {
                                        ((Client)pairss.getValue()).turnRight();
                                    } 
                                }
                                
                            }
                        }
                        // Maze fire synchronization
                        else if (packetFromServer.type == MazePacket.MAZE_FIRE) {
                        	if(fireQueue.isEmpty())
                                fireQueue.add(packet);
                        }
                        else if (packetFromServer.type == MazePacket.MAZE_TICK) {
                              System.out.println("sequence number - " + num + ": tick");
                              maze.updateProjectile();
                              if (fireQueue.isEmpty()==false){
                            	  MazePacket firePacket = fireQueue.remove();
                            	  if (firePacket.name.equals(client.getName())) {
                                      System.out.println("sequence number - " + num + ": client fire");
                                      client.fire();
                                  }
                                  else {
                                      System.out.println("sequence number - " + num + ": remote fire");
                                      
                                      Iterator itt = remote.entrySet().iterator();
                                      while (itt.hasNext()) {
                                          Map.Entry pairss = (Map.Entry)itt.next();
                                          String remoteUser = (String)pairss.getKey();
                                          if (firePacket.name == remoteUser) {
                                            ((Client)pairss.getValue()).fire();
                                          } 
                                      }
                                      
                                  }
                              }
                        }
                        clientQueue.remove(num);
                    }
                }
            }
            out.close();
            in.close();
            socket.close();
		}
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
    
    // register GUIClient
    public void addClientToServer() {
        try {
            MazePacket packetToServer = new MazePacket();
            packetToServer.type = MazePacket.MAZE_REGISTER;
            packetToServer.name = client.getName();
            packetToServer.location = maze.getClientPoint(client);
            packetToServer.direction = client.getOrientation();
            out.writeObject(packetToServer);
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    // respawn GUIClient
    public static void respawn(Client killer, Client dead, Point p, Direction d) {
        if (client.getName() == dead.getName()) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_RESPAWN;
                packetToServer.name = dead.getName();
                packetToServer.location = p;
                packetToServer.direction = d;
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }

    // GUIClient keypress
    public static void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            Mazewar.quit();
        // Up-arrow moves forward.
        } 
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_FORWARD;
                packetToServer.name = client.getName();
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Down-arrow moves backward.
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_BACKUP;
                packetToServer.name = client.getName();
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Left-arrow turns left.
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_TURNLEFT;
                packetToServer.name = client.getName();
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Right-arrow turns right.
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_TURNRIGHT;
                packetToServer.name = client.getName();
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }    
        // Spacebar fires.
        else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            try {
                MazePacket packetToServer = new MazePacket();
                packetToServer.type = MazePacket.MAZE_FIRE;
                packetToServer.name = client.getName();
                out.writeObject(packetToServer);
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }
    
}