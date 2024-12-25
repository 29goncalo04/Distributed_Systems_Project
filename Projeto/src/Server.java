import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 3; 
    public static final ClientManager cmanager = new ClientManager();
    private static final Queue<Socket> waitingQueue = new LinkedList<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition isAvailable = lock.newCondition();
    private static int activeSessions = 0;
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                lock.lock();
                try{
                    if(activeSessions >= MAX_SESSIONS){
                        waitingQueue.add(clientSocket);
                    } else {
                        activeSessions++;
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

    private static void startWorker(Socket clienSocket){
        Worker worker = new Worker(clienSocket, cmanager);
        Thread t = new Thread(worker);
        t.start();
    }

    public static void notifySessionEnd(){
        lock.lock();
        try{
            activeSessions--;
            if(!waitingQueue.isEmpty()){
                Socket nextClient = waitingQueue.poll(); //usamos poll por causa do caso em que a lista é vazia
                activeSessions++;
                startWorker(nextClient);
            } else {
                isAvailable.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}