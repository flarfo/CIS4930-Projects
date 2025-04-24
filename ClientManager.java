import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ClientManager {
    private static final Map<Integer, Process> clients = new HashMap<>();
    private static final Scanner scanner = new Scanner(System.in);
    private static int nextId = 1;
    private static String serverAddr, serverPort;
    private static final String CLIENT_DIR = "clients";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ClientManager <server_address> <port>");
            return;
        }

        serverAddr = args[0];
        serverPort = args[1];
        
        // Create clients directory structure
        setupDirectories();

        System.out.println("Client Manager");
        System.out.println("Commands: spawn <2|4|8>, list, send <id> <msg>, kill <id>, killall, exit");

        while (true) {
            System.out.print("> ");
            String cmd = scanner.nextLine().trim();

            if (cmd.equals("exit")) {
                killAllClients();
                break;
            } else if (cmd.startsWith("spawn ")) {
                try {
                    int count = Integer.parseInt(cmd.substring(6).trim());
                    if (count == 2 || count == 4 || count == 8) {
                        spawnClients(count);
                    } else {
                        System.out.println("Error: Use 2, 4, or 8 clients only");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid number");
                }
            } else if (cmd.equals("list")) {
                listClients();
            } else if (cmd.equals("killall")) {
                killAllClients();
            } else if (cmd.startsWith("kill ")) {
                try {
                    int id = Integer.parseInt(cmd.substring(5).trim());
                    killClient(id);
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid client ID");
                }
            } else if (cmd.startsWith("send ")) {
                String[] parts = cmd.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("Usage: send <client_id> <message>");
                } else {
                    try {
                        int id = Integer.parseInt(parts[1]);
                        sendToClient(id, parts[2]);
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Invalid client ID");
                    }
                }
            } else {
                System.out.println("Unknown command");
            }
        }
    }
    
    private static void setupDirectories() {
        // Create clients directory
        File clientsDir = new File(CLIENT_DIR);
        if (!clientsDir.exists()) {
            if (clientsDir.mkdir()) {
                System.out.println("Created clients directory");
            } else {
                System.out.println("Failed to create clients directory");
                System.exit(1);
            }
        }
        
        // Create downloads directory
        File downloadsDir = new File(clientsDir, "downloads");
        if (!downloadsDir.exists()) {
            if (downloadsDir.mkdir()) {
                System.out.println("Created downloads directory");
            } else {
                System.out.println("Failed to create downloads directory");
                System.exit(1);
            }
        }
    }

    private static void spawnClients(int count) {
        System.out.println("Starting " + count + " clients...");
        for (int i = 0; i < count; i++) {
            try {
                // Create client-specific directory
                String clientId = String.valueOf(nextId);
                File clientDir = new File(CLIENT_DIR, "client_" + clientId);
                if (!clientDir.exists()) {
                    clientDir.mkdir();
                }
                
                // Create log file
                File logFile = new File(clientDir, "output.log");
                
                // Create symbolic link to downloads dir (or just use the shared one)
                File clientDownloads = new File(clientDir, "downloads");
                if (!clientDownloads.exists()) {
                    // Create client-specific downloads folder that points to main downloads
                    clientDownloads.mkdir();
                }
                
                // Use the correct path to Client.java
                ProcessBuilder pb = new ProcessBuilder("java", "-cp", "../..", "Client", serverAddr, serverPort);
                // Set working directory to client directory
                pb.directory(clientDir);
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                
                // Use FileOutputStream to ensure log is properly flushed
                FileOutputStream logStream = new FileOutputStream(logFile);
                
                // Start the process
                Process process = pb.start();
                
                // Set up output redirection with auto-flush
                redirectStreamToFile(process.getInputStream(), logStream);
                redirectStreamToFile(process.getErrorStream(), logStream);
                
                clients.put(nextId, process);
                System.out.println("Started client " + nextId + " (output in " + logFile.getPath() + ")");
                nextId++;

                // Avoid overwhelming the server
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("Error starting client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void redirectStreamToFile(InputStream inputStream, FileOutputStream outputStream) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, n);
                    outputStream.flush();
                }
            } catch (IOException e) {
                // Ignore errors when client exits
            }
        }).start();
    }

    private static void listClients() {
        if (clients.isEmpty()) {
            System.out.println("No active clients.");
            return;
        }

        System.out.println("Active clients:");
        Iterator<Map.Entry<Integer, Process>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Process> entry = it.next();
            boolean isAlive = entry.getValue().isAlive();
            System.out.println("Client " + entry.getKey() + ": " + 
                (isAlive ? "RUNNING" : "TERMINATED"));
            
            // Remove terminated clients from our map
            if (!isAlive) {
                it.remove();
            }
        }
    }

    private static void killClient(int id) {
        Process process = clients.get(id);
        if (process == null) {
            System.out.println("No client with ID " + id);
            return;
        }

        try {
            // Try graceful exit
            sendToClient(id, "bye");
            Thread.sleep(500);

            // Force kill if still alive
            if (process.isAlive()) {
                process.destroy();
                System.out.println("Killed client " + id);
            } else {
                System.out.println("Client " + id + " exited gracefully");
            }

            clients.remove(id);
        } catch (Exception e) {
            System.out.println("Error killing client: " + e.getMessage());
        }
    }

    private static void killAllClients() {
        System.out.println("Killing all clients...");
        List<Integer> ids = new ArrayList<>(clients.keySet());
        for (int id : ids) {
            killClient(id);
        }
    }

    private static void sendToClient(int id, String message) {
        Process process = clients.get(id);
        if (process == null || !process.isAlive()) {
            System.out.println("No active client with ID " + id);
            return;
        }

        try {
            PrintWriter writer = new PrintWriter(process.getOutputStream(), true);
            writer.println(message);
            System.out.println("Sent '" + message + "' to client " + id);
        } catch (Exception e) {
            System.out.println("Error sending to client: " + e.getMessage());
        }
    }
}