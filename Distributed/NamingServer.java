import java.net.*;
import java.io.*;
import java.util.*;

// This Naming Service server is the first thing to run. 
// Mazewars are clients to this server.
public class NamingServer {
    public static void main(String[] args) throws IOException {
        ServerSocket namingPort = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                namingPort = new ServerSocket(Integer.parseInt(args[0]));
            } 
            else {
                System.out.println(args.length);
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
            // pass parameters that are needed in the handler thread
            new NamingServerHandlerThread(namingPort.accept()).start();
        }

        namingPort.close(); 
    }
}
