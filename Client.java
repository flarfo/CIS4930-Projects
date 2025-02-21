import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.time.*;

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
        List<Double> roundTripTimes = new ArrayList<>();

        // Read until "bye" is input
        while (!message.equals("bye")) {
            try {
                if (message.equals("bye")) {
                    response = rin.readUTF();
                    System.out.println(response);
                    break;
                }

                // Read user input
                System.out.print("Enter a string: ");
                message = in.readLine();

                // Write to server + time it
                long startTime = System.nanoTime();

                out.writeUTF(message);
                out.flush();

                // Read from server
                response = rin.readUTF();
                System.out.println(response);

                long endTime = System.nanoTime();
                double roundTripTime = (endTime - startTime) / 10000000.0;  // Convert to milliseconds
                System.out.println("Round-trip time: " + roundTripTime + " ms");
                roundTripTimes.add(roundTripTime);

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
        if (args.length != 2) {
            System.out.println("Usage: java Client <server_address> <port_number>");
            return;
        }
        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);
        new Client(serverAddress, port);
    }
}