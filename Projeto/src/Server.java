import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 10; // Adjust based on S
    private static final Map<Integer, Socket> clientConnections = new HashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Chamar o clientManager
                // Thread t = new Thread(clientManager)
                // t.start()
            }
        } finally {
            // fechar a puta
        }
    }

    public static void removeClient(Client client) {
        clientConnections.remove(client.id);
    }

    // static byte[] get(String key) {
    //     return dataStore.get(key);
    // }

    // static void put(String key, byte[] value) {
    //     dataStore.put(key, value);
    // }

    // static Map<String, byte[]> multiGet(Set<String> keys) {
    //     Map<String, byte[]> result = new HashMap<>();
    //     for (String key : keys) {
    //         if (dataStore.containsKey(key)) {
    //             result.put(key, dataStore.get(key));
    //         }
    //     }
    //     return result;
    // }

    // static void multiPut(Map<String, byte[]> pairs) {
    //     dataStore.putAll(pairs);
    // }

    // static byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
    //     while (!Arrays.equals(dataStore.get(keyCond), valueCond)) {
    //         Thread.sleep(100); // Busy wait, replace with better signaling if needed
    //     }
    //     return dataStore.get(key);
    // }
}