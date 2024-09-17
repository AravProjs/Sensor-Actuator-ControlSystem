package cpen221.mp3.client;

import cpen221.mp3.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {

    private final int clientId;
    private String email;
    private String serverIP;
    private int serverPort;

    private List<Entity> entityList = new ArrayList<>();
    private List<Thread> entityThreads = new ArrayList<>();

    // you would need additional fields to enable functionalities required for this class

    public Client(int clientId, String email, String serverIP, int serverPort) {
        this.clientId = clientId;
        this.email = email;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public int getClientId() {
        return clientId;
    }

    /**
     * Registers an entity to the client and starts it running as a process.
     *
     * @return true if the entity is new and gets successfully registered, false if the Entity is already registered
     */
    public boolean addEntity(@NotNull Entity entity) {
        if(!entity.registerForClient(this.clientId)) return false;
        entity.setEndpoint(this.serverIP, this.serverPort);

        entityList.add(entity);

        Thread entityThread = new Thread(entity);
        entityThread.start();
        entityThreads.add(entityThread);

        return true;
    }

    // sends a request to the server
    public void sendRequest(Request request) {


        // note that Request is a complex object that you need to serialize before sending
        String currentIP;
        int currentPort;
        currentIP = serverIP;
        currentPort = serverPort;

        Socket sendSocket = null;
        try {
            sendSocket = new Socket(currentIP, currentPort);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()));
            out.println("client," + clientId);
            out.flush();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(sendSocket.getOutputStream());
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(request.getRequestType() == RequestType.ANALYSIS) {
            ObjectInputStream ois;
            Object returnedObject;
            try {
                ois = new ObjectInputStream(sendSocket.getInputStream());
                returnedObject = ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if(request.getRequestCommand() == RequestCommand.ANALYSIS_GET_ALL_ENTITIES) {
                List<Integer> allEntities = (List<Integer>) returnedObject;
                System.out.println("All Entities: " + allEntities);
            } else if (request.getRequestCommand() == RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY) {
                Integer mostActiveEntity = (Integer) returnedObject;
                System.out.println("Most Active Entity ID: " + mostActiveEntity);
            } else if(request.getRequestCommand() == RequestCommand.ANALYSIS_GET_LATEST_EVENTS) {
                List<Integer> latestEvents = (List<Integer>) returnedObject;
                System.out.println("Latest Events: " + latestEvents);
            } else if(request.getRequestCommand() == RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW) {
                List<Integer> eventsInWindow = (List<Integer>) returnedObject;
                System.out.println("Events within the window: " + eventsInWindow);
            } else if(request.getRequestCommand() == RequestCommand.ANALYSIS_GET_LOGS) {
                List<Integer> logList = (List<Integer>) returnedObject;
                System.out.println("All Entities Logged: " + logList);
            }
        }


    }

    /**
     * Keeps all entities for this client alive, Useful for putting at the end of main to keep the entities running perpetually.
     */
    public void joinAllEntities() {
        for(Thread entityThread : entityThreads) {
            try {
                entityThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String toString() {
        return clientId + "," + email + "," + serverIP + "," + serverPort;
    }

}