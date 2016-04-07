import java.net.*;
import java.io.*;

public class MazewarServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                serverSocket = new ServerSocket(Integer.parseInt(args[0]));    
            } 
            else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } 
        catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
        
        // Thread to send to server
        new MazewarServerSendHandler().start();
        // Thread to add missile tick to queue
    	new MazewarServerTickHandler().start();

        // Run thread 
        while (listening) {
        	new MazewarServerHandlerThread(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
}