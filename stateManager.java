public class stateManager {
    private static int clientCount = 0; // Number of clients connected to the server

    public static synchronized void incrementClientCount() {
        clientCount++;
    }

    public static synchronized void decrementClientCount() {
        clientCount--;
    }

    public static synchronized int getClientCount() {
        return clientCount;
    }
}
