import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 1; 
    public static final ClientManager cmanager = new ClientManager();
    private static final Queue<Socket> waitingQueue = new LinkedList<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static int activeSessions = 0;
    
    public static int getActiveSessions() {
        return activeSessions;
    }

    public static void decrementActiveSessions() {
        Server.activeSessions--;
    }

    public static void incrementActiveSessions() {
        Server.activeSessions++;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                lock.lock();
                try{
                    if(getActiveSessions() >= MAX_SESSIONS){
                        waitingQueue.add(clientSocket);
                    } else {
                        incrementActiveSessions();;
                        startWorker(clientSocket);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally{
            if(serverSocket != null){
                serverSocket.close();
            }
        }
    }

    private static void startWorker(Socket clientSocket){
        // Inicia o trabalhador
        Worker worker = new Worker(clientSocket, cmanager);
        Thread t = new Thread(worker);
        t.start();
    }

    public static void notifySessionEnd(){
        lock.lock();
        try{
            if(!waitingQueue.isEmpty()){
                Socket nextClient = waitingQueue.poll(); //usamos poll por causa do caso em que a lista é vazia
                incrementActiveSessions();
                startWorker(nextClient);
            }
        } finally {
            lock.unlock();
        }
    }
}