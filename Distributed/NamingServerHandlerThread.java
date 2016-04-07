import java.net.*;
import java.io.*;
import java.util.*;


public class NamingServerHandlerThread extends Thread {
    private TreeMap<String, Integer> port = new TreeMap<String, Integer>(); // port
    private static TreeMap<String, Integer> port1 = new TreeMap<String, Integer>(); // port
    private TreeMap<String, String> host = new TreeMap<String, String>(); // host
    private static TreeMap<String, String> host1 = new TreeMap<String, String>(); // host

    public static ArrayList<ObjectOutputStream> toAllClient = new ArrayList<ObjectOutputStream>(); // socket output stream

	private Socket socket = null;

	public NamingServerHandlerThread(Socket socket) {
		super("NamingServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new NamingServerHandlerThread to handle client");
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
				/* create a packet to send reply back to client */
//				
//				if (packetFromClient.type == MazePacket.MAZE_POSITIONS){
//                    MazePacket packetToClient = new MazePacket();
//					packetToClient.type = MazePacket.MAZE_POSITIONSREPLY;
//					packetToClient.loc1 = loc1;
//					
//					toClient.writeObject(packetToClient);
//					
//				}
//				
				if (packetFromClient.type == MazePacket.MAZE_REGISTER) {

                    MazePacket packetToClient = new MazePacket();
                    packetToClient.type = MazePacket.MAZE_REPLY;

					host.put(packetFromClient.name, packetFromClient.hostt);
                    host1.put(packetFromClient.name, packetFromClient.hostt);
                    port.put(packetFromClient.name, packetFromClient.portt);
                    port1.put(packetFromClient.name, packetFromClient.portt);

                    toAllClient.add(toClient);

                    packetToClient.host1 = host1;
                    packetToClient.host = host;
                    packetToClient.port1 = port1;
                    packetToClient.port = port;
                    
                    for (int i=0; i<toAllClient.size(); i++) {
                        toAllClient.get(i).writeObject(packetToClient);
                        if (toAllClient.size()==1){
                        	MazePacket tickLeader = new MazePacket();
                        	tickLeader.type=MazePacket.MAZE_TICKLEADER;
                            toAllClient.get(i).writeObject(tickLeader);
                        }
                    } 
				}
            }

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
