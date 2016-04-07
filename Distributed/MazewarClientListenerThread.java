import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MazewarClientListenerThread extends Thread {
	Socket socket = null;
	private static int ok = 0;

	private static int key = 0;
	private static int cf = 0; 
	private static int rf = 0;
	//private TreeMap<Integer, String> message = new TreeMap<Integer, String>();
	//private TreeMap<Integer, String> map = new TreeMap<Integer, String>();
	//private static TreeMap<Integer, TreeMap> queue = new TreeMap<Integer, TreeMap>(); // socket output stream 
	private static ArrayList<Tuple> queue = new ArrayList<Tuple>();
	public static BlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();
	
	private static PrintWriter writer = null;
	public static Lock printLock = new ReentrantLock(true);

//	public static Queue<MazePacket> fireQueue = new LinkedList<MazePacket>();
	public static Lock mutex = new ReentrantLock(true);
	
	public static int broadcastACK=0;
	public static Lock broadcastACKLock = new ReentrantLock(true);

	public static HashMap<String, Location> remoteLocation = new HashMap<String, Location>(); // remote players

	public MazewarClientListenerThread(Socket socket) {
		super("MazewarClientListenerThread");
		this.socket = socket;
		System.out.println("Created new MazewarClientListenerThread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			MazePacket packetFromClient;
			
			if (writer==null){
				writer = new PrintWriter(MazewarClientHandlerThread.client.getName(), "UTF-8");
			}
			
			//Timer timer = new Timer();
 			//timer.schedule(new SendFireTick(), 0, 200);
 			
			//String leader = MazewarClientHandlerThread.host2.firstEntry().getKey();
			String leader = "a";
			
			//String leader = MazewarClientHandlerThread.client.getName();
//			if (MazewarClientHandlerThread.client.getName().equals(leader)) {
//				Timer timer = new Timer();
// 				timer.schedule(new SendFireTick(), 0, 200);
// 			}
			
			while ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				MazePacket packetToClient = new MazePacket();
				
				// if asked loc & dir, send those information to the requester
                if (packetFromClient.type == MazePacket.MAZE_REMOTE_SETUP) {
                	int safe = 1;
                	// if newer user wants to occupy my location
                	if (packetFromClient.location.x == MazewarClientHandlerThread.maze.getClientPoint(MazewarClientHandlerThread.client).x
                		&&
                		packetFromClient.location.y == MazewarClientHandlerThread.maze.getClientPoint(MazewarClientHandlerThread.client).y) {
                		MazewarClientHandlerThread.rejectRemotes(packetFromClient.name);
                		safe = 0;
                	}
                	// if newer user wants to occupy other remote client's location
                	Iterator a = remoteLocation.entrySet().iterator();
		            while (a.hasNext()) {
		                Map.Entry pairx = (Map.Entry)a.next();
		                Location l = (Location)pairx.getValue();
		                if (l.x == packetFromClient.location.x && l.y == packetFromClient.location.y) {
		                	safe = 0;
		                }
		            }
                	if (safe == 1) {
	                    MazewarClientHandlerThread.replyRemotes(packetFromClient.name);
	                    Client remoteClient = new RemoteClient(packetFromClient.name); 
	                    {
	                    	Location loc = new Location(packetFromClient.location.x, packetFromClient.location.y);
	                    	remoteLocation.put(packetFromClient.name, loc);
	                    	MazewarClientHandlerThread.remote.put(packetFromClient.name, remoteClient);
	                        MazewarClientHandlerThread.maze.addRemoteClient(remoteClient, packetFromClient.location, packetFromClient.direction);
	                    }
	                }
                }
                // if they reply, set up the remote client
                if (packetFromClient.type == MazePacket.MAZE_REMOTE_REPLY) {
                	int safe = 1;
                	// if someone already occupied my location
                	if (packetFromClient.location.x == MazewarClientHandlerThread.maze.getClientPoint(MazewarClientHandlerThread.client).x
                		&&
                		packetFromClient.location.y == MazewarClientHandlerThread.maze.getClientPoint(MazewarClientHandlerThread.client).y) {
                		MazewarClientHandlerThread.maze.removeClient(MazewarClientHandlerThread.client);
	                    MazewarClientHandlerThread.maze.addClient(MazewarClientHandlerThread.client);
	                    MazewarClientHandlerThread.getRemotes();
                	}
                	// if newer user wants to occupy other remote client's location
                	Iterator a = remoteLocation.entrySet().iterator();
		            while (a.hasNext()) {
		                Map.Entry pairx = (Map.Entry)a.next();
		                Location l = (Location)pairx.getValue();
		                if (l.x == packetFromClient.location.x && l.y == packetFromClient.location.y) {
		                	safe = 0;
		                }
		            }
                	if (safe == 1) {
	                    Client remoteClient = new RemoteClient(packetFromClient.name); 
	                    {
	                    	Location loc = new Location(packetFromClient.location.x, packetFromClient.location.y);	
	                    	remoteLocation.put(packetFromClient.name, loc);
	                    	MazewarClientHandlerThread.remote.put(packetFromClient.name, remoteClient);
	                        MazewarClientHandlerThread.maze.addRemoteClient(remoteClient, packetFromClient.location, packetFromClient.direction);
	                    }
                	}
                }
                // if location overlap found, recreate local GUIClient and retry
                if (packetFromClient.type == MazePacket.MAZE_REMOTE_REJECT) {
                    MazewarClientHandlerThread.maze.removeClient(MazewarClientHandlerThread.client);
                    MazewarClientHandlerThread.maze.addClient(MazewarClientHandlerThread.client);
                    MazewarClientHandlerThread.getRemotes();
                }

				if (packetFromClient.type == MazePacket.MAZE_TICK) {
                    MazewarClientHandlerThread.updateTick();
                    
				}
				if (packetFromClient.type == MazePacket.MAZE_TICKLEADER) {
                	new MazewarServerTickHandler().start();
				}
				if (packetFromClient.type == MazePacket.MAZE_RESPAWN) {
					System.out.println(packetFromClient.name + " RESPAWNS");
                    if (MazewarClientHandlerThread.remote.containsKey(packetFromClient.name)) {
                        Client me = MazewarClientHandlerThread.remote.get(packetFromClient.name);
                        Direction d = packetFromClient.direction;
                        // Force constant direction when mazes respawn
                        while (!me.getOrientation().equals(d)) {
                            me.turnRight();
                        }     
                    }
                    else {
                        // Lock keys such that users don't interrupt while mazez fix their directions
                        ((GUIClient)MazewarClientHandlerThread.client).keyLock = 0;
                    }
                }

				// Receiving request from other clients
				if (packetFromClient.type == MazePacket.MAZE_REQUEST) {
					//System.out.println("got request");
					int iWin;
					int moveLC;

					
					
					mutex.lock();
//					if (!MazewarClientHandlerThread.request){
					MazewarClientHandlerThread.requestLock.lock();
					
					MazewarClientHandlerThread.counterLock.lock();
					moveLC=MazewarClientHandlerThread.moveLC;
					if(MazewarClientHandlerThread.counter<packetFromClient.counter){
						MazewarClientHandlerThread.counter=packetFromClient.counter;
					}
					MazewarClientHandlerThread.counter++;
                    MazewarClientHandlerThread.counterLock.unlock();

					if (MazewarClientHandlerThread.request==false){
						MazewarClientHandlerThread.sendOK(packetFromClient.name);
//						System.out.println("Send OK to " + packetFromClient.name);
					}
					else{
//						System.out.println("I'm in a critical section.");
//						System.out.println("My LC = " + moveLC + ", " + packetFromClient.name + " LC = " + packetFromClient.counter);

						if (moveLC<packetFromClient.counter){
//							System.out.println("My LC < " + packetFromClient.name + " LC");
							sendQueue.put(packetFromClient.name);
						}
						else if (moveLC==packetFromClient.counter){
//							System.out.println("My LC == " + packetFromClient.name + " LC");
							if (MazewarClientHandlerThread.client.getName().compareTo(packetFromClient.name)<0){
//								System.out.println("My name wins");
								sendQueue.put(packetFromClient.name);
							}
							else{
//								System.out.println("My name loses");
								MazewarClientHandlerThread.sendOK(packetFromClient.name);
//								System.out.println("Send OK to " + packetFromClient.name);
							}
						}
						else{
//							System.out.println("My LC > " + packetFromClient.name + " LC");
							MazewarClientHandlerThread.sendOK(packetFromClient.name);
//							System.out.println("Send OK to " + packetFromClient.name);
						}
					}

					MazewarClientHandlerThread.requestLock.unlock();
					mutex.unlock();
				}
				
				

				// Broadcast confiming its movement
				if (packetFromClient.type == MazePacket.MAZE_BROADCAST) {
						Iterator itt = MazewarClientHandlerThread.remote.entrySet().iterator();
	                    while (itt.hasNext()) {
	                        Map.Entry pairss = (Map.Entry)itt.next();
	                        String remoteUser = (String)pairss.getKey();
	                        if (packetFromClient.name.equals(remoteUser)) {
	                        	if (packetFromClient.move == MazePacket.MAZE_FORWARD) {
									((Client)pairss.getValue()).forward();
									printLock.lock();
									writer.println(packetFromClient.name + ": forward");
									writer.flush();
									printLock.unlock();
								}
								else if (packetFromClient.move == MazePacket.MAZE_BACKUP) {
									((Client)pairss.getValue()).backup();
									printLock.lock();
									writer.println(packetFromClient.name + ": back");
									writer.flush();
									printLock.unlock();
								}
								else if (packetFromClient.move == MazePacket.MAZE_TURNLEFT) {
									((Client)pairss.getValue()).turnLeft();
									printLock.lock();
									writer.println(packetFromClient.name + ": left");
									writer.flush();
									printLock.unlock();
								}
								else if (packetFromClient.move == MazePacket.MAZE_TURNRIGHT) {
									((Client)pairss.getValue()).turnRight();
									printLock.lock();
									writer.println(packetFromClient.name + ": right");
									writer.flush();
									printLock.unlock();
								}
								else if (packetFromClient.move == MazePacket.MAZE_FIRE) {
									MazewarClientHandlerThread.fire(packetFromClient);
									printLock.lock();
									writer.println(packetFromClient.name + ": fire");
									writer.flush();
									printLock.unlock();
								}
	                        } 
	                    }
	                    
	                    MazewarClientHandlerThread.broadcastACK(packetFromClient.name);
//					}
				}

				// Other clients send back OK
				if (packetFromClient.type == MazePacket.MAZE_OK) {
					
					
					MazewarClientHandlerThread.counterLock.lock();
					MazewarClientHandlerThread.counter++;
					packetFromClient.counter++;
					if(MazewarClientHandlerThread.counter<packetFromClient.counter){
						MazewarClientHandlerThread.counter=packetFromClient.counter;
					}
                    MazewarClientHandlerThread.counterLock.unlock();
                    
					mutex.lock();
					ok++;
//					System.out.println("RECEIVED " + ok + " OK from " + packetFromClient.name);
					int num = MazewarClientHandlerThread.n - 1;
					if (ok == num) {
						// 
						MazewarClientHandlerThread.broadcast(MazewarClientHandlerThread.client.getName());
						
						if (MazewarClientHandlerThread.move == MazePacket.MAZE_FORWARD) {
							printLock.lock();
							writer.println(MazewarClientHandlerThread.client.getName() + ": forward");
							writer.flush();
							printLock.unlock();
						}
						else if (MazewarClientHandlerThread.move == MazePacket.MAZE_BACKUP) {
							printLock.lock();
							writer.println(MazewarClientHandlerThread.client.getName() + ": back");
							writer.flush();
							printLock.unlock();
						}
						else if (MazewarClientHandlerThread.move == MazePacket.MAZE_TURNLEFT) {
							printLock.lock();
							writer.println(MazewarClientHandlerThread.client.getName() + ": left");
							writer.flush();
							printLock.unlock();
						}
						else if (MazewarClientHandlerThread.move == MazePacket.MAZE_TURNRIGHT) {
							printLock.lock();
							writer.println(MazewarClientHandlerThread.client.getName() + ": right");
							writer.flush();
							printLock.unlock();
						}
						else if (MazewarClientHandlerThread.move == MazePacket.MAZE_FIRE) {
							printLock.lock();
							writer.println(MazewarClientHandlerThread.client.getName() + ": fire");
							writer.flush();
							printLock.unlock();
						}
						ok = 0;
					}
					mutex.unlock();
					// broadcast its movement
					
				}
				
				if (packetFromClient.type == MazePacket.MAZE_BROADCASTACK) {
					int num = MazewarClientHandlerThread.n - 1;
					broadcastACKLock.lock();
					broadcastACK++;
//					System.out.println("broadcast ACK " + broadcastACK);
					
					if(broadcastACK==num){
						broadcastACK=0;
						

						MazewarClientHandlerThread.requestLock.lock();
						MazewarClientHandlerThread.request=false;
						MazewarClientHandlerThread.requestLock.unlock();
						
						//broadcast OK
						mutex.lock();
						while(!sendQueue.isEmpty()){
							String sendName = sendQueue.take();
							MazewarClientHandlerThread.sendOK(sendName);
//							System.out.println("Send OK to " + sendName);
						}
						mutex.unlock();
						
					}
					
					broadcastACKLock.unlock();
					
				
				}

        	}
			/* cleanup when client exits */
			fromClient.close();
			socket.close();
			writer.close();
		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
		
}