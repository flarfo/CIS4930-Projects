import java.net.*;
import java.io.*;
import java.util.*;
import java.time.*;

public class Server {  
    private Socket socket;
    private ServerSocket serverSocket;
    private DataInputStream in; // Read (client) input from the socket
    private DataOutputStream out; // Write (server) response to client

    public Server(int port) {
        try
        {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on PORT " + port);
            System.out.println("Awaiting client...");

            socket = serverSocket.accept();
            System.out.println("Client accepted from " + socket.getLocalAddress());

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
            String message = "";

            while (true)
            {
                try
                {
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

            // Close connection
            System.out.println("Closing connection...");

            serverSocket.close();
            in.close();
            out.close();

            System.out.println("Connection closed.");
        }
        catch (IOException err) {
            System.out.println(err);
        }
    }

    public static void main(String args[])
    {
        // Error handle for when no arguments are passed!
        if (args.length != 1) {
            System.out.println("Usage: java Server <port_number>");
            return;
        }
        Server server = new Server(Integer.parseInt(args[0]));
    }
}