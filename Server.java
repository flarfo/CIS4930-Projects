import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class Server {
    private static final int MAX_CLIENTS = 16;
    private static final String IMAGE_DIR = "./images";

    private static final AtomicInteger ACTIVE = new AtomicInteger();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port_number>");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (ACTIVE.get() < MAX_CLIENTS) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            ACTIVE.incrementAndGet();
            try {
                in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                System.out.println("Connected: " + socket.getRemoteSocketAddress());
                out.writeUTF("Hello!");

                for ( ; ; ) {
                    String line = in.readUTF();

                    if ("bye".equalsIgnoreCase(line.trim())) {
                        out.writeUTF("disconnected");
                        break;
                    }

                    if (!line.startsWith("SEND")) {
                        out.writeUTF("Please type a different command");
                        continue;
                    }

                    List<String> order = parseOrder(line);
                    sendBatch(order);
                }

            } catch (IOException e) {
                System.out.println("Client I/O error: " + e.getMessage());
            } finally {
                ACTIVE.decrementAndGet();
                closeQuietly();
                System.out.println("Closed: " + socket.getRemoteSocketAddress());
            }
        }

        private List<String> parseOrder(String cmd) {
            String[] parts = cmd.trim().split("\\s+");
            List<String> list = new ArrayList<>();
            if (parts.length == 1) {                     // no filenames given
                for (int i = 1; i <= 10; i++)
                    list.add("sample0" + i + ".bmp");
            } else {
                list.addAll(Arrays.asList(parts).subList(1, parts.length));
            }
            return list;
        }

        private void sendBatch(List<String> order) throws IOException {
            out.writeUTF("OK");
            out.writeInt(order.size());

            for (String fname : order) {
                File file = new File(IMAGE_DIR, fname);
                if (!file.exists()) {
                    out.writeUTF("NF");
                    out.writeUTF(fname);
                    continue;
                }

                out.writeUTF("FILE");
                out.writeUTF(fname);
                out.writeLong(file.length());

                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = bis.read(buf)) != -1) out.write(buf, 0, n);
                }
            }
            out.flush();
        }

        private void closeQuietly() {
            try { if (in    != null) in.close();  } catch (IOException ignored) {}
            try { if (out   != null) out.close(); } catch (IOException ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
