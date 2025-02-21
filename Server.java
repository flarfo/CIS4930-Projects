import java.net.*;
import java.io.*;

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

            // Reads message from client until "bye" is sent
            while (!message.equals("bye"))
            {
                try
                {
                    message = in.readUTF();
                    System.out.println(message);

                    // TODO: Project specs #1 (capitalize alphabet and return to client)

                    // Send response to client
                    out.writeUTF("SERVER: " + message);
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
        Server server = new Server(Integer.parseInt(args[0]));
    }
}