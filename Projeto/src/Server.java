import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 10; // Adjust based on S
    private static final Map<Integer, Socket> clientConnections = new HashMap<>();
    public static final ClientManager cmanager = new ClientManager();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Worker worker = new Worker(clientSocket, cmanager, MAX_SESSIONS);
                Thread t = new Thread(worker);
                t.start();
            }
        } finally{
            serverSocket.close();
            System.out.println("Server socket has been closed");
        }
    }
}