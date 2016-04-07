import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Map;

public class MazewarServerTickHandler extends Thread{
	
	public static final int NUM_TICKS=30;
	
	public void run() {

		for (int i=0; i<NUM_TICKS; i++){
			MazewarClientHandlerThread.sendTicks();
			MazewarClientHandlerThread.updateTick();
            //sleep
            try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		MazewarClientHandlerThread.electTickLeader();		
	}
}
