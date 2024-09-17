package cpen221.mp3.handler;

import cpen221.mp3.client.Client;
import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.TimeToProcess;
import cpen221.mp3.server.Server;
import org.apache.commons.collections4.queue.PredicatedQueue;

import java.io.*;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Integer.parseInt;

class MessageHandlerThread implements Runnable {
    private final Socket incomingSocket;
    private final List<Server> serverList;
    private final PriorityBlockingQueue<TimeToProcess> eventQueue;
    private final List<Thread> serverThreads;

    public MessageHandlerThread(Socket incomingSocket, List<Server> serverList, PriorityBlockingQueue<TimeToProcess> eventQueue, List<Thread> serverThreads) {
        this.incomingSocket = incomingSocket;
        this.serverList = serverList;
        this.eventQueue = eventQueue;
        this.serverThreads = serverThreads;
    }

    @Override
    public void run() {
        // handle the client request or entity event here
        // and deal with exceptions if needed
        Object requestOrEvent;
        String type;
        String[] typeIDArr;
        double currentTime = System.currentTimeMillis();
        int ID;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
            String typeID = in.readLine();
            ObjectInputStream ois = new ObjectInputStream(incomingSocket.getInputStream());
            requestOrEvent = ois.readObject();
            typeIDArr = typeID.split(",");
            type = typeIDArr[0];
            ID = parseInt(typeIDArr[1]);
        } catch (IOException | ClassNotFoundException e) {
            // Do nothing LOL
            return;
        }
        Server thisServer = null;

        boolean newServerWasMade = false;
        synchronized (serverList) {
            for (Server server : serverList) {
                if (server.getClientID() == ID) {
                    thisServer = server;
                    break;
                }
            }
            if (thisServer == null) {
                Client client = new Client(ID, "misty@pennertechnologies.com", "127.0.0.1", 0);
                thisServer = new Server(client);
                serverList.add(thisServer);
                newServerWasMade = true;
            }
        }
        if(newServerWasMade) {
            Thread serverThread = new Thread(thisServer);
            synchronized (serverThreads) {
                serverThreads.add(serverThread);
            }
            serverThread.start();
        }

        double timeToProcess = thisServer.getMaxWaitTime() * 1000 + currentTime;

        if(requestOrEvent instanceof Request request) {
            TimeToProcess newRequest = new TimeToProcess(timeToProcess, request, ID, incomingSocket);
            eventQueue.put(newRequest);
        } else if(requestOrEvent instanceof Event event) {
            if(typeIDArr.length == 3) {
                synchronized (thisServer.actuator_port_map) {
                    synchronized (thisServer.actuator_IP_map) {
                        if (!thisServer.actuator_port_map.containsKey(event.getEntityId())) {
                            thisServer.actuator_port_map.put(event.getEntityId(), parseInt(typeIDArr[2]));
                        }
                        if (!thisServer.actuator_IP_map.containsKey(event.getEntityId())) {
                            thisServer.actuator_IP_map.put(event.getEntityId(), incomingSocket.getInetAddress().getHostAddress());
                        }
                    }
                }
            }
            TimeToProcess newEvent = new TimeToProcess(timeToProcess, event);
            eventQueue.put(newEvent);
        }
    }
}