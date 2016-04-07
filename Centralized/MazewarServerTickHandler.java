public class MazewarServerTickHandler extends Thread{
	
	public void run() {

		while(true){
		
			//Queue Tick
            MazePacket packetToClient = new MazePacket();
            packetToClient.type = MazePacket.MAZE_TICK;
             
            MazewarServerHandlerThread.seqNum++;
            MazewarServerHandlerThread.serverQueue.put(MazewarServerHandlerThread.seqNum, packetToClient);        

            //sleep
            try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
