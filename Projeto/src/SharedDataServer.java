import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Main Server Class
public class SharedDataServer {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 10; // Adjust based on S
    private static final Map<String, byte[]> dataStore = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> activeClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                synchronized (activeClients) {
                    if (activeClients.size() < MAX_SESSIONS) {
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        activeClients.add(clientHandler);
                        threadPool.execute(clientHandler);
                    } else {
                        clientSocket.close(); // Reject connection if max sessions are reached
                    }
                }
            }
        } finally {
            threadPool.shutdown();
        }
    }

    public static synchronized void removeClient(ClientHandler clientHandler) {
        activeClients.remove(clientHandler);
    }

    static synchronized byte[] get(String key) {
        return dataStore.get(key);
    }

    static synchronized void put(String key, byte[] value) {
        dataStore.put(key, value);
    }

    static synchronized Map<String, byte[]> multiGet(Set<String> keys) {
        Map<String, byte[]> result = new HashMap<>();
        for (String key : keys) {
            if (dataStore.containsKey(key)) {
                result.put(key, dataStore.get(key));
            }
        }
        return result;
    }

    static synchronized void multiPut(Map<String, byte[]> pairs) {
        dataStore.putAll(pairs);
    }

    static synchronized byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        while (!Arrays.equals(dataStore.get(keyCond), valueCond)) {
            Thread.sleep(100); // Busy wait, replace with better signaling if needed
        }
        return dataStore.get(key);
    }
}

// Client Handler Class
class ClientHandler implements Runnable {
    private final Socket clientSocket;

    ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            // Authentication (placeholder logic)
            String username = in.readUTF();
            String password = in.readUTF();
            if (!authenticate(username, password)) {
                out.writeUTF("Authentication failed");
                return;
            }

            out.writeUTF("Authentication successful");

            // Handle client requests
            boolean running = true;
            while (running) {
                String command = in.readUTF();
                switch (command) {
                    case "put": {
                        String key = in.readUTF();
                        int valueLength = in.readInt();
                        byte[] value = new byte[valueLength];
                        in.readFully(value);
                        SharedDataServer.put(key, value);
                        out.writeUTF("OK");
                        break;
                    }
                    case "get": {
                        String key = in.readUTF();
                        byte[] value = SharedDataServer.get(key);
                        if (value != null) {
                            out.writeInt(value.length);
                            out.write(value);
                        } else {
                            out.writeInt(-1);
                        }
                        break;
                    }
                    case "multiPut": {
                        int numEntries = in.readInt();
                        Map<String, byte[]> pairs = new HashMap<>();
                        for (int i = 0; i < numEntries; i++) {
                            String key = in.readUTF();
                            int valueLength = in.readInt();
                            byte[] value = new byte[valueLength];
                            in.readFully(value);
                            pairs.put(key, value);
                        }
                        SharedDataServer.multiPut(pairs);
                        out.writeUTF("OK");
                        break;
                    }
                    case "multiGet": {
                        int numKeys = in.readInt();
                        Set<String> keys = new HashSet<>();
                        for (int i = 0; i < numKeys; i++) {
                            keys.add(in.readUTF());
                        }
                        Map<String, byte[]> result = SharedDataServer.multiGet(keys);
                        out.writeInt(result.size());
                        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
                            out.writeUTF(entry.getKey());
                            out.writeInt(entry.getValue().length);
                            out.write(entry.getValue());
                        }
                        break;
                    }
                    case "exit": {
                        running = false;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SharedDataServer.removeClient(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean authenticate(String username, String password) {
        // Placeholder: Replace with real authentication logic
        return username.equals("user") && password.equals("pass");
    }
}
