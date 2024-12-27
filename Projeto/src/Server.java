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
                    if(activeSessions >= MAX_SESSIONS){
                        waitingQueue.add(clientSocket);
                        new Thread(() -> {
                            sendWaitingMessage(clientSocket);
                        }).start();
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

    private static void startWorker(Socket clientSocket){
        try {
            // Notifica o cliente que ele saiu da fila e pode interagir
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeUTF("You are no longer in the queue. You can now interact with the server.");
            out.flush();
    
            // Inicia o trabalhador
            Worker worker = new Worker(clientSocket, cmanager);
            Thread t = new Thread(worker);
            t.start();
        } catch (IOException e) {
            System.out.println("Failed to notify client: " + e.getMessage());
            notifySessionEnd();
        }
    }

    private static void sendWaitingMessage(Socket clientSocket) {
        // Envia uma mensagem informando ao cliente que ele está na fila
        new Thread(() -> {
            try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                out.writeUTF("Server is full. You are in the waiting queue. Please wait until a slot becomes available. Use [exit] to leave the queue");
                out.flush();
                lock.lock();
                try{
                    while (activeSessions >= MAX_SESSIONS) { 
                        isAvailable.await(); 
                    }
                    activeSessions++;
                    startWorker(clientSocket);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
                
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