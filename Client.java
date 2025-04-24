import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final int RUNS_PER_STATS = 5;    // print stats every N runs
    private Socket socket;
    private BufferedReader userIn;
    private DataInputStream rin;
    private DataOutputStream out;
    private final List<Double> rtts = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        socket  = new Socket(host, port);
        userIn  = new BufferedReader(new InputStreamReader(System.in));
        rin     = new DataInputStream(socket.getInputStream());
        out     = new DataOutputStream(socket.getOutputStream());

        System.out.println("Connected to " + host + ":" + port);
        System.out.println(rin.readUTF());            // “Hello!”
    }

    public void loop() throws IOException {
        String cmd;
        while (true) {
            System.out.print("Type SEND or bye (or a filename): ");
            cmd = userIn.readLine();
            if (cmd == null) break;

            if ("bye".equalsIgnoreCase(cmd.trim())) {
                out.writeUTF("bye");
                out.flush();
                System.out.println(rin.readUTF());                 
                break;
            }

            if ("SEND".equalsIgnoreCase(cmd.trim())) {
                doBatchSend();
                continue;
            }
            doSingleFile(cmd);
        }

        closeQuietly();
        System.out.println("Connection closed.");
    }


    private void doBatchSend() throws IOException {
        List<Integer> order = new ArrayList<>();
        Random rand = new Random();
        while (order.size() < 10) {
            int n = rand.nextInt(10) + 1;
            if (!order.contains(n)) order.add(n);
        }
        StringBuilder sb = new StringBuilder("SEND");
        for (int n : order){
            if (n < 10){
                sb.append("sample0").append(n).append(".bmp");
            }
            else {
                sb.append("sample").append(n).append(".bmp");
            }
            
        } 

        long start = System.nanoTime();
        out.writeUTF(sb.toString());
        out.flush();

        String resp = rin.readUTF();
        if (!"OK".equals(resp)) { System.out.println("Server: "+resp); return; }
        int files = rin.readInt();

        File dir = new File("./downloads"); if (!dir.exists()) dir.mkdirs();

        for (int i = 0; i < files; i++) {
            String marker = rin.readUTF();
            if ("NF".equals(marker)) { System.out.println("Missing: "+rin.readUTF()); continue; }
            if (!"FILE".equals(marker)) continue;

            String fname = rin.readUTF();
            long size    = rin.readLong();

            try (FileOutputStream fos = new FileOutputStream(new File(dir, fname))) {
                byte[] buf = new byte[8192];
                long   r   = 0;
                while (r < size) {
                    int n = rin.read(buf, 0, (int)Math.min(buf.length, size - r));
                    fos.write(buf, 0, n);
                    r += n;
                }
            }
            System.out.println("Saved " + fname + " (" + size + " bytes)");
        }

        addRtt(start);
    }

    private void doSingleFile(String filename) throws IOException {
        long start = System.nanoTime();
        out.writeUTF(filename);
        out.flush();

        String resp = rin.readUTF();
        if ("File not found".equals(resp)) {
            System.out.println(resp);
            return;
        }
        if (!"OK".equals(resp)) {
            System.out.println("Server: " + resp); return;
        }

        File dir = new File("./downloads"); if (!dir.exists()) dir.mkdirs();
        long size = rin.readLong();

        try (FileOutputStream fos = new FileOutputStream(new File(dir, filename))) {
            byte[] buf = new byte[4096];
            long   r   = 0;
            while (r < size) {
                int n = rin.read(buf);
                fos.write(buf, 0, n);
                r += n;
            }
        }
        System.out.println("File downloaded: " + filename);
        addRtt(start);
    }

    private void addRtt(long startNs) {
        double ms = (System.nanoTime() - startNs) / 1_000_000.0;
        System.out.printf("Round-trip: %.2f ms%n", ms);
        rtts.add(ms);
        if (rtts.size() == RUNS_PER_STATS) {
            printStats();
            rtts.clear();
        }
    }

    private void printStats() {
        double min = Collections.min(rtts);
        double max = Collections.max(rtts);
        double mean = rtts.stream().mapToDouble(d -> d).average().orElse(0);
        double sd = Math.sqrt(rtts.stream().mapToDouble(d -> (d-mean)*(d-mean)).sum()/rtts.size());
        System.out.printf("Stats (%d runs)  min %.2f  max %.2f  mean %.2f  σ %.2f ms%n",
                          RUNS_PER_STATS, min, max, mean, sd);
    }

    private void closeQuietly() {
        try { userIn.close(); } catch (IOException ignored) {}
        try { rin.close();    } catch (IOException ignored) {}
        try { out.close();    } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <server_address> <port_number>");
            return;
        }
        try {
            new Client(args[0], Integer.parseInt(args[1])).loop();
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
