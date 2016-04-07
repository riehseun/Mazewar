import java.util.Iterator;
import java.util.Map;

public class MazewarServerSendHandler extends Thread {
	
		public void run() {
			
		while(true){
			// dequeue and broadcast
			if (MazewarServerHandlerThread.serverQueue.isEmpty() == false) {
		        MazePacket packet = null;
		        Integer num = 0;
		        String name = "";
		        Iterator it = MazewarServerHandlerThread.serverQueue.entrySet().iterator();
		        while (it.hasNext()) {
		            Map.Entry pairs = (Map.Entry)it.next();
		            packet = (MazePacket)pairs.getValue();
		            name = packet.name;
		            num = (Integer)pairs.getKey();
		            if (packet.type == MazePacket.MAZE_FORWARD) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_FORWARD;
		                    packetToClient.name = packet.name;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    } 
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else if (packet.type == MazePacket.MAZE_BACKUP) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_BACKUP;
		                    packetToClient.name = packet.name;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    }
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else if (packet.type == MazePacket.MAZE_TURNLEFT) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_TURNLEFT;
		                    packetToClient.name = packet.name;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    }
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else if (packet.type == MazePacket.MAZE_TURNRIGHT) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_TURNRIGHT;
		                    packetToClient.name = packet.name;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    }
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else if (packet.type == MazePacket.MAZE_FIRE) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_FIRE;
		                    packetToClient.name = packet.name;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    }
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else if (packet.type == MazePacket.MAZE_TICK) {
		                try {
		                    MazePacket packetToClient = new MazePacket();
		                    packetToClient.type = MazePacket.MAZE_TICK;
		                    packetToClient.num = num;
		                    for (int i=0; i<MazewarServerHandlerThread.toAllClient.size(); i++) {
		                    	MazewarServerHandlerThread.toAllClient.get(i).writeObject(packetToClient);
		                    }
		                }
		                catch (Exception e) {
		                    e.printStackTrace();
		                }
		            }
		            else {
		                System.out.println("unknown packet type");
		            }
		            MazewarServerHandlerThread.serverQueue.remove(num);
		        } // close while
		    } // close if
		}
	}
}
