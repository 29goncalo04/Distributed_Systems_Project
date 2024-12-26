import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 1; 
    public static final ClientManager cmanager = new ClientManager();
    private static final Queue<Socket> waitingQueue = new LinkedList<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition isAvailable = lock.newCondition();
    private static int activeSessions = 0;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                lock.lock();
                try{
                    System.out.println("Waiting queue size before: " + waitingQueue.size());
                    if(activeSessions >= MAX_SESSIONS){
                        waitingQueue.add(clientSocket);
                        sendWaitingMessage(clientSocket);
                        System.out.println("Waiting queue size after: " + waitingQueue.size());
                        isAvailable.await();
                    }
                    activeSessions++;
                    System.out.println("a tua mae :" + activeSessions);
                    startWorker(clientSocket);
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
        Worker worker = new Worker(clientSocket, cmanager);
        Thread t = new Thread(worker);
        t.start();
    }

    private static void sendWaitingMessage(Socket clientSocket) {
        // Envia uma mensagem informando ao cliente que ele está na fila
        new Thread(() -> {
            try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                out.writeUTF("Server is full. You are in the waiting queue. Please wait until a slot becomes available.");
                out.flush();
                
            } catch (IOException e) {
                System.out.println("Error sending waiting message: " + e.getMessage());
            }
        }).start();
    }

    public static void notifySessionEnd(){
        lock.lock();
        try{
            activeSessions--;
            if(!waitingQueue.isEmpty()){
                Socket nextClient = waitingQueue.poll(); //usamos poll por causa do caso em que a lista é vazia
                startWorker(nextClient);
            } else {
                isAvailable.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}