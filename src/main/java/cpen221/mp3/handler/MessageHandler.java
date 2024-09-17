package cpen221.mp3.handler;

import cpen221.mp3.event.Event;
import cpen221.mp3.event.TimeToProcess;
import cpen221.mp3.server.Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class MessageHandler implements Runnable {
    private ServerSocket serverSocket;
    private int port;

    List<Server> serverList = new ArrayList<>();
    PriorityBlockingQueue<TimeToProcess> eventQueue = new PriorityBlockingQueue<>(10, (x, y) -> {
        if(x.getTimeStamp() < y.getTimeStamp()) {
            x.setTimeAtWhichToProcess(y.getTimeAtWhichToProcess());
        }
        return Double.compare(x.getTimeStamp(), y.getTimeStamp());
    });
    List<Thread> serverThreads = new ArrayList<>();

    // you may need to add additional private fields and methods to this class

    public MessageHandler(int port) {
        this.port = port;
    }

    public void run() {
        // the following is just to get you started
        // you may need to change it to fit your implementation
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket incomingSocket = serverSocket.accept();
                System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());

                // create a new thread to handle the client request or entity event
                Thread handlerThread = new Thread(new MessageHandlerThread(incomingSocket, serverList, eventQueue, serverThreads));
                handlerThread.start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // you would need to initialize the RequestHandler with the port number
        // and then start it here
        MessageHandler server = new MessageHandler(4377);
        Thread serverThread = new Thread(server);
        serverThread.start();

        while(true) {
            TimeToProcess firstEventOrRequest = null;
            try {
                firstEventOrRequest = server.eventQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(firstEventOrRequest.getTimeAtWhichToProcess() >= System.currentTimeMillis() ) {
                for(Server i : server.serverList) {
                    if(i.getClientID() == firstEventOrRequest.getClientId()) {
                        i.processIncomingEventOrRequest(firstEventOrRequest);
                        break;
                    }
                }
            } else {
                server.eventQueue.put(firstEventOrRequest);
            }
        }
    }
}
