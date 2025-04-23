import java.net.*;
import java.io.*;
import java.util.*;
import java.time.*;

public class Server {  

    private static final int maxClients = 16; // Maximum number of clients allowed

    public static void main(String args[])
    {
        // Error handle for when no arguments are passed!
        if (args.length != 1) {
            System.out.println("Usage: java Server <port_number>");
            return;
        }
        int port = Integer.parseInt(args[0]); // Port number to listen on

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            // Server is always listening for new connections
            // Spec is to exit when all clients exited
            // therefore keep track of number of clients and exit when all are done
            boolean started = false;

            while (stateManager.getClientCount() < maxClients) {
                if (!started) {
                    System.out.println("Server started, waiting for clients...");
                    started = true;
                } else if (stateManager.getClientCount() == 0) {
                    System.out.println("All clients disconnected, shutting down server.");
                    break;
                }
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                new ClientHandler(socket).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in; // Read (client) input from the socket
    private DataOutputStream out; // Write (server) response to client

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() { 
        try {
            stateManager.incrementClientCount(); // Increment client count when a new client connects
            System.out.println("Client accepted from " + socket.getLocalAddress());

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
            String message = "";

            while (true){
                try{
                    message = in.readUTF();
                    // Terminate when "bye" received
                    if (message.equals("bye")) {
                        out.writeUTF("disconnected");
                        break;
                    }

                    File file = new File("./images", message);
                    if (!file.exists()) {
                        out.writeUTF("File not found");
                        continue;
                    }

                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file)); // Read files on server database (./images)
                    byte[] buffer = new byte[4096];
                    int bytesRead;
            
                    out.writeUTF("OK");
                    out.flush();

                    out.writeLong(file.length());
                    out.flush();

                    while ((bytesRead = bis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }

                    bis.close();
                }
                catch(IOException err)
                {
                    System.out.println(err);
                }
            }
        }
        catch (IOException err) {
            System.out.println(err);
        } finally {
            // Decrement client count when a client disconnects
            stateManager.decrementClientCount();
            // Close connection
            System.out.println("Closing connection...");
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException err) {
                System.out.println(err);
            }
            System.out.println("Connection closed.");
        }
    }
}