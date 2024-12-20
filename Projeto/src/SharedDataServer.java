import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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
