import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MazewarClientHandlerThread extends Thread {
    private static TreeMap<String, String> host2 = new TreeMap<String, String>(); // host 
    private static TreeMap<String, Integer> port2 = new TreeMap<String, Integer>(); // port

    private static TreeMap<String, ObjectOutputStream> toAllClient = new TreeMap<String, ObjectOutputStream>();
    private static TreeMap<String, ObjectInputStream> fromAllClient = new TreeMap<String, ObjectInputStream>();

    public static HashMap<String, Client> remote = new HashMap<String, Client>(); // remote players

    public static int counter = 0;
    public static Lock counterLock = new ReentrantLock(true);
    
    public static int moveLC=0;
    public static boolean request = false;
    public static Lock requestLock = new ReentrantLock(true);
    
    public static int n; 
    private static Queue<MazePacket> fireQueue = new LinkedList<MazePacket>();
    public static Lock _fireLock = new ReentrantLock(true);
    
    public static int move;
    

    
    public static ArrayList<String> election = new ArrayList<String>(); // (public for election)

    public static Lock writeLock = new ReentrantLock(true);

    Socket socket = null;
    static ObjectOutputStream out = null; // only one instance of outputstream per client
    static ObjectInputStream in = null;
    Socket namingSocket = null;
    static ObjectOutputStream namingOut = null; // only one instance of outputstream per client
    static ObjectInputStream namingIn = null;

    public static Client client = null; // only one local client 
    static Maze maze = null;
    String name = "";
    String host = "";
    Integer port = 0;

    Integer namingPort = 4000;

	public MazewarClientHandlerThread(Client client, Maze maze, String name, String host, Integer port) {
		super("MazewarClientHandlerThread");
		System.out.println("Created new MazewarClientHandlerThread");
        this.client = client;
        this.maze = maze;
        this.name = name;
        this.host = host;
        this.port = port;
        try {
            namingSocket = new Socket(host, namingPort);
            namingOut = new ObjectOutputStream(namingSocket.getOutputStream());
            namingIn = new ObjectInputStream(namingSocket.getInputStream());       
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        }
		System.out.println("Created new MazewarClientHandlerThread to handle client");
	}

	public void run() {
        boolean gotByePacket = false;
        election.add(client.getName());

		try {
            MazePacket packetFromServer;
            
        	new MoveThread().start();
            while ((packetFromServer = (MazePacket) namingIn.readObject()) != null) {

            	// If client must be elected as first tick leader
            	if (packetFromServer.type == MazePacket.MAZE_TICKLEADER){
                    System.out.println("NEW TICK LEADER");
                	new MazewarServerTickHandler().start();
                }
            	
//            	if (packetFromServer.type == MazePacket.MAZE_POSITIONSREPLY){
//                    System.out.println("NEW TICK LEADER");
//                	new MazewarServerTickHandler().start();
//                }
            	
                // If clients want to register
                if (packetFromServer.type == MazePacket.MAZE_REPLY) {

                    // treemap for host
                    host2 = packetFromServer.host1;
                    if (!host2.containsKey(packetFromServer.host.firstEntry().getKey())) {
                        String value = packetFromServer.host.firstEntry().getValue();
                        String key = packetFromServer.host.firstEntry().getKey();
                        host2.put(key, value);
                    }
                    System.out.println("complete host: " + host2);

                    // treemap for port
                    port2 = packetFromServer.port1;
                    if (!port2.containsKey(packetFromServer.port.firstEntry().getKey())) {
                        Integer value = packetFromServer.port.firstEntry().getValue();
                        String key = packetFromServer.port.firstEntry().getKey();
                        port2.put(key, value);
                    }
                    System.out.println("complete port: " + port2);
               
                    Iterator t = port2.entrySet().iterator();
                    while (t.hasNext()) {
                        Map.Entry pairx = (Map.Entry)t.next();
                        String x = (String)pairx.getKey();
                        Integer portp = (Integer)pairx.getValue();
                        String hosth = null;

                        Iterator u = host2.entrySet().iterator();
                        while (u.hasNext()) {
                            Map.Entry pairy = (Map.Entry)u.next();
                            String y = (String)pairy.getKey();
                            if (x.equals(y)) {
                                hosth = (String)pairy.getValue();
                            }
                        }
                        
                        if (!toAllClient.containsKey(x) && !x.equals(client.getName())) {
                            Socket newsocket = new Socket(hosth, portp);
                            ObjectOutputStream out = new ObjectOutputStream(newsocket.getOutputStream());
                            toAllClient.put(x, out);    
                            n = toAllClient.size() + 1;
                        }
                    }

                                        
                    /*
                    // check if client is not GUIClient or already added
                    if (!remote.containsKey(x) && !x.equals(client.getName())) {
                        Client remoteClient = new RemoteClient(x); 
                        {
                            remote.put(x, remoteClient);
                            maze.addRemoteClient(remoteClient, location, direction); // add remote client
                            //System.out.println("now facing: " + remoteClient.getOrientation());
                        }    
                    }
                    */
                }

                getRemotes();

            }
            namingOut.close();
            namingIn.close();
            namingSocket.close();
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
    
    public static void getRemotes() {
        try {
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_REMOTE_SETUP;
            packetToClient.name = client.getName();
            packetToClient.location = maze.getClientPoint(client);
            packetToClient.direction = client.getOrientation();

            writeLock.lock();
            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                out.writeObject(packetToClient);
            }
            writeLock.unlock();
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public static void replyRemotes(String requester_name) {
        try {
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_REMOTE_REPLY;
            packetToClient.location = maze.getClientPoint(client);
            packetToClient.direction = client.getOrientation();
            packetToClient.name = client.getName();

            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                String requester = (String)pairx.getKey();
                if (requester_name.equals(requester)) {
                    ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                    writeLock.lock();
                    out.writeObject(packetToClient);
                    writeLock.unlock();
                    continue;
                }
            }
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public static void rejectRemotes(String rejected_name) {
        try {
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_REMOTE_REJECT;
            packetToClient.location = maze.getClientPoint(client);
            packetToClient.direction = client.getOrientation();
            packetToClient.name = client.getName();

            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                String requester = (String)pairx.getKey();
                if (rejected_name.equals(requester)) {
                    ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                    writeLock.lock();
                    out.writeObject(packetToClient);
                    writeLock.unlock();
                    continue;
                }
            }
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    // register GUIClient
    public void addClientToServer() {
        try {
            MazePacket packetToServer = new MazePacket();
            packetToServer.type = MazePacket.MAZE_REGISTER;
            packetToServer.name = client.getName();
            packetToServer.portt = port;
            packetToServer.hostt = Inet4Address.getLocalHost().getHostAddress();
            packetToServer.location = maze.getClientPoint(client);
            packetToServer.direction = client.getOrientation();
            
            namingOut.writeObject(packetToServer);
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public static void sendOK(String requester_name) {
        try {
        	
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_OK;
            packetToClient.name = client.getName();
            
            counterLock.lock();
            counter++;
            packetToClient.counter = counter;
            counterLock.unlock();
            
            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                String requester = (String)pairx.getKey();
                if (requester_name.equals(requester)) {
                    ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                    writeLock.lock();
                    out.writeObject(packetToClient);
                    writeLock.unlock();
                    continue;
                }
            }
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }
    
    public static void broadcastACK(String requester_name) {
        try {
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_BROADCASTACK;
            packetToClient.name = requester_name;
            
            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                String requester = (String)pairx.getKey();
                if (requester_name.equals(requester)) {
                    ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                    writeLock.lock();
                    out.writeObject(packetToClient);
                    writeLock.unlock();
                    continue;
                }
            }
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public static void broadcast(String name) {
        try {
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_BROADCAST;
            packetToClient.name = name;
            packetToClient.move = move;
            
            writeLock.lock();
            Iterator a = toAllClient.entrySet().iterator();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                out.writeObject(packetToClient);
            }
            writeLock.unlock();
            
            if (move == MazePacket.MAZE_FORWARD) {
				MazewarClientHandlerThread.client.forward();
			}
			else if (move == MazePacket.MAZE_BACKUP) {
				MazewarClientHandlerThread.client.backup();
			}
			else if (move == MazePacket.MAZE_TURNLEFT) {
				MazewarClientHandlerThread.client.turnLeft();
			}
			else if (move == MazePacket.MAZE_TURNRIGHT) {
				MazewarClientHandlerThread.client.turnRight();
			}
			else if (move == MazePacket.MAZE_FIRE) {
				MazePacket packet = new MazePacket();
				packet.name = name;
				MazewarClientHandlerThread.fire(packet);
			}
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }
    
    
    // respawn GUIClient
    public static void respawn(Client killer, Client dead, Point p, Direction d) {
        if (client.getName() == dead.getName()) {
            
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_RESPAWN;
            packetToClient.name = client.getName();
            packetToClient.location = p;
            packetToClient.direction = d;
            
            Iterator a = toAllClient.entrySet().iterator();
            writeLock.lock();
            while (a.hasNext()) {
                Map.Entry pairx = (Map.Entry)a.next();
                ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
                try {
					out.writeObject(packetToClient);
				} catch (IOException e) {
					e.printStackTrace();
				} 
            }
            writeLock.unlock();
            ((GUIClient)client).keyLock = 0;
        }
    }

    // GUIClient keypress
    public static void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            Mazewar.quit();
        } 
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
            try {
            	
            	if (requestLock.tryLock()){
	                if (request == false) {
	                    request = true;
	                    MazePacket packetToClient = new MazePacket();
	                    move = MazePacket.MAZE_FORWARD;
	                    packetToClient.type = MazePacket.MAZE_REQUEST;
	                    packetToClient.name = client.getName();
	                    counterLock.lock();
	                    counter++;
	                    packetToClient.counter = counter;
	                    moveLC=counter;
	                    counterLock.unlock();

                        writeLock.lock();
	                    Iterator t = toAllClient.entrySet().iterator();
	                    while (t.hasNext()) {
	                        Map.Entry pairx = (Map.Entry)t.next();
	                        
	                        ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                        out.writeObject(packetToClient);
	                        
	                    }
	                    writeLock.unlock();
	                    if (toAllClient.isEmpty()){
	                    	client.forward();
	                    	request=false;
	                    }
	                }
	                requestLock.unlock();
            	}
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Down-arrow moves backward.
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            try {
            	if (requestLock.tryLock()){
	                if (request == false) {
		                request = true;
	                    MazePacket packetToClient = new MazePacket();
	                    move = MazePacket.MAZE_BACKUP;
	                    packetToClient.type = MazePacket.MAZE_REQUEST;
	                    packetToClient.name = client.getName();
	                    counterLock.lock();
	                    counter++;
	                    packetToClient.counter = counter;
	                    moveLC=counter;
	                    counterLock.unlock();
	                    
                        writeLock.lock();
	                    Iterator t = toAllClient.entrySet().iterator();
	                    while (t.hasNext()) {
	                        Map.Entry pairx = (Map.Entry)t.next();
	                        
	                        ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                        //System.out.println("send request, lamport clock: " + counter);
	                        //System.out.println(out);
	                        out.writeObject(packetToClient);
	                    }
                        writeLock.unlock();
	                    if (toAllClient.isEmpty()){
	                    	client.backup();
	                    	request=false;
	                    }
	                }
	                requestLock.unlock();
            	}
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Left-arrow turns left.
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            try {

              if (requestLock.tryLock()){
            	  if (request == false) {
	                    request = true;
	                    MazePacket packetToClient = new MazePacket();
	                    move = MazePacket.MAZE_TURNLEFT;
	                    packetToClient.type = MazePacket.MAZE_REQUEST;
	                    packetToClient.name = client.getName();
	                    counterLock.lock();
	                    counter++;
	                    packetToClient.counter = counter;
	                    moveLC=counter;
	                    counterLock.unlock();

                        writeLock.lock();
	                    Iterator t = toAllClient.entrySet().iterator();
	                    while (t.hasNext()) {
	                        Map.Entry pairx = (Map.Entry)t.next();
	                        
	                        ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                        out.writeObject(packetToClient);
	                    }
                        writeLock.unlock();
	                    if (toAllClient.isEmpty()){
	                    	client.turnLeft();
	                    	request=false;
	                    }
                	}
            	  	requestLock.unlock();
                }
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        // Right-arrow turns right.
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            try {
            	if (requestLock.tryLock()){
	                if (request == false) {
		                    request = true;
	                    MazePacket packetToClient = new MazePacket();
	                    move = MazePacket.MAZE_TURNRIGHT;
	                    packetToClient.type = MazePacket.MAZE_REQUEST;
	                    packetToClient.name = client.getName();
	                    counterLock.lock();
	                    counter++;
	                    packetToClient.counter = counter;
	                    moveLC=counter;
	                    counterLock.unlock();

                        writeLock.lock();
	                    Iterator t = toAllClient.entrySet().iterator();
	                    while (t.hasNext()) {
	                        Map.Entry pairx = (Map.Entry)t.next();
	                        ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                        out.writeObject(packetToClient);
	                    }
                        writeLock.unlock();
                        if (toAllClient.isEmpty()){
                        	client.turnRight();
                        	request=false;
                        }
	                }
                    requestLock.unlock();
                }
            } 
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }    
        // Spacebar fires.
        else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            try {
            	if (requestLock.tryLock()){
	                if (request == false) {
		                    request = true;
	                    MazePacket packetToClient = new MazePacket();
	                    move = MazePacket.MAZE_FIRE;
	                    packetToClient.type = MazePacket.MAZE_REQUEST;
	                    packetToClient.name = client.getName();
	                    counterLock.lock();
	                    counter++;
	                    packetToClient.counter = counter;
	                    moveLC=counter;
	                    counterLock.unlock();

                        writeLock.lock();
	                    Iterator t = toAllClient.entrySet().iterator();
	                    while (t.hasNext()) {
	                        Map.Entry pairx = (Map.Entry)t.next();
	                        ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                        out.writeObject(packetToClient);
	                    }
                        writeLock.unlock();
                        if (toAllClient.isEmpty()){
                        	fire(packetToClient);
                        	request=false;
                        }
	                }
	                requestLock.unlock();
                }
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        
    }
    public static void sendTicks(){
        MazePacket packetToClient = new MazePacket();
        packetToClient.type = MazePacket.MAZE_TICK;
    	writeLock.lock();
    	Iterator t = toAllClient.entrySet().iterator();
        while (t.hasNext()) {
            Map.Entry pairx = (Map.Entry)t.next();
            ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
            //System.out.println("send request, lamport clock: " + counter);
            try {
				out.writeObject(packetToClient);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		writeLock.unlock();
    }
    
    public static void electTickLeader(){
        MazePacket packetToClient = new MazePacket();
        packetToClient.type = MazePacket.MAZE_TICKLEADER;
    	String tempName=null;
    	ObjectOutputStream out=null;
    	
    	// check if empty
    	if (toAllClient.isEmpty()){
    		//reelect self
        	new MazewarServerTickHandler().start();
        	return;
    	}
    	
    	
    	for (Map.Entry entry: toAllClient.entrySet()){
    		tempName = (String)entry.getKey();
    		if (tempName.compareTo(client.getName())>0){
    			
    			out = (ObjectOutputStream)entry.getValue();
    			try {
    				writeLock.lock();
    				out.writeObject(packetToClient);
    				writeLock.unlock();
    				return;
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    				
    	        }
        	}
    	}
    	

    	
    	out = (ObjectOutputStream) toAllClient.firstEntry().getValue();
    	try {
			out.writeObject(packetToClient);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
        }
    }
    
    public static void updateTick(){
    	maze.updateProjectile();
    	_fireLock.lock();
        if (fireQueue.isEmpty()==false){
	      	  MazePacket firePacket = fireQueue.remove();
	      	  System.out.println(firePacket.name + " FIRE");
	      	  if (firePacket.name.equals(client.getName())) {
	                System.out.println("client fire");
	                client.fire();
	          }
	          else {
	                System.out.println("remote fire");
	                
	                Iterator itt = remote.entrySet().iterator();
	                while (itt.hasNext()) {
	                    Map.Entry pairss = (Map.Entry)itt.next();
	                    String remoteUser = (String)pairss.getKey();
	                    if (firePacket.name.equals(remoteUser)) {
	                      ((Client)pairss.getValue()).fire();
	                    } 
	                }
	                
	          }
        }
        _fireLock.unlock();
    }
    
    public static void fire(MazePacket packetFromClient){
    	_fireLock.lock();
		if(fireQueue.isEmpty()) {
            fireQueue.add(packetFromClient);
        }
		_fireLock.unlock();
    }
    public static void moveLeft(){
    	try {
    		if (requestLock.tryLock()){
                if (request == false) {
	                    request = true;
	                MazePacket packetToClient = new MazePacket();
	                move = MazePacket.MAZE_TURNLEFT;
	                packetToClient.type = MazePacket.MAZE_REQUEST;
	                packetToClient.name = client.getName();
	                counterLock.lock();
	                counter++;
	                packetToClient.counter = counter;
	                moveLC=counter;
	                counterLock.unlock();
	                
//	                System.out.println("REQUEST LC = " + moveLC);
	
	                Iterator t = toAllClient.entrySet().iterator();
	                writeLock.lock();
	                while (t.hasNext()) {
	                    Map.Entry pairx = (Map.Entry)t.next();
	                    ObjectOutputStream out = (ObjectOutputStream)pairx.getValue();
	                    out.writeObject(packetToClient);
	                }
	                writeLock.unlock();
                //request=false;
                }
                requestLock.unlock();
            }
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }
    }
}
