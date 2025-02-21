import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private BufferedReader in; // Read user input from command line
    private DataInputStream rin; // Read (server) input from the socket
    private DataOutputStream out; // Write (client) output to the server

    public Client(String addr, int port) {
        try {
            // Try connection at addr, port
            socket = new Socket(addr, port);

            in = new BufferedReader(new InputStreamReader(System.in));
            rin = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Connection established at " + addr + " on PORT " + port);
        }
        catch (UnknownHostException err) {
            System.out.println(err);
        }
        catch (IOException err) {
            System.out.println(err);
        }

        String message = ""; // String to read message from user input
        String response = ""; // String to read response from server
        
        // Read until "bye" is input
        while (!message.equals("bye")) {
            try {
                // Read user input
                message = in.readLine();
                // Write to server
                out.writeUTF(message);

                // Read from server
                response = rin.readUTF();
                System.out.println(response);
            }
            catch (IOException err) {
                System.out.println(err);
            }
        }

        // Close connection
        try {
            System.out.println("Closing connection...");

            in.close();
            rin.close();
            out.close();
            socket.close();

            System.out.println("Connected closed.");
        }
        catch (IOException err) {
            System.out.println(err);
        }
    }

    public static void main(String args[])
    {
        Client client = new Client("127.0.0.1", Integer.parseInt(args[0]));
    }
}