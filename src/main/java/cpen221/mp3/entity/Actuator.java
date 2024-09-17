package cpen221.mp3.entity;

import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.server.SeverCommandToActuator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Actuator implements Entity, Runnable {
    private final int id;
    private int clientId;
    private final String type;
    private boolean state;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)
    // the following specifies the http endpoint that the actuator should send events to
    private String serverIP = null;
    private int serverPort = 0;
    // the following specifies the http endpoint that the actuator should be able to receive commands on from server
    private String host = null;
    private int port;
    private ServerSocket serverSocket;
    private static AtomicInteger portOffset = new AtomicInteger(4378);
    private Random randomNumber = new Random();

    /**
     * Constructs an Actuator object where the
     * the client ID remains unregistered (-1).
     *
     * @param id         The ID of the actuator.
     * @param type       The type of the actuator.
     * @param init_state The initial state of the actuator.
     */
    public Actuator(int id, String type, boolean init_state) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
    }


    /**
     * Constructs an Actuator object with an ID, client ID, type, and initial state.
     *
     * @param id         The ID of the actuator.
     * @param clientId   The client ID associated with the actuator.
     * @param type       The type of the actuator.
     * @param init_state The initial state of the actuator.
     */
    public Actuator(int id, int clientId, String type, boolean init_state) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
    }

    /**
     * Constructs an Actuator object where
     * the client ID remains unregistered (-1).
     *
     * @param id         The ID of the actuator.
     * @param type       The type of the actuator.
     * @param init_state The initial state of the actuator.
     * @param serverIP   The server IP address.
     * @param serverPort The server port number.
     */
    public Actuator(int id, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Constructs an Actuator object.
     *
     * @param id         The ID of the actuator.
     * @param clientId   The client ID associated with the actuator.
     * @param type       The type of the actuator.
     * @param init_state The initial state of the actuator.
     * @param serverIP   The server IP address.
     * @param serverPort The server port number.
     */
    public Actuator(int id, int clientId, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Retrieves the ID of the actuator.
     *
     * @return The ID of the actuator.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the client ID associated with the actuator.
     *
     * @return The client ID associated with the actuator.
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Retrieves the type of the actuator.
     *
     * @return The type of the actuator.
     */
    public String getType() {
        return type;
    }

    /**
     * Indicates whether the object is an actuator.
     *
     * @return True since this object is an actuator.
     */
    public boolean isActuator() {
        return true;
    }

    /**
     * Retrieves the current state of the actuator.
     *
     * @return The current state of the actuator.
     */
    public boolean getState() {
        return state;
    }

    /**
     * Retrieves the IP address of the server associated with the actuator.
     *
     * @return The IP address of the server associated with the actuator.
     */
    public String getIP() {
        return host;
    }

    /**
     * Retrieves the port number of the server associated with the actuator.
     *
     * @return The port number of the server associated with the actuator.
     */
    public int getPort() {
        return port;
    }

    /**
     * Updates the state of the actuator.
     *
     * @param new_state The new state to be set for the actuator.
     */
    public void updateState(boolean new_state) {
        this.state = new_state;
    }

    /**
     * Registers the actuator for the given client
     * 
     * @return true if the actuator is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public boolean registerForClient(int clientId) {
        if(this.clientId == clientId) return true;
        if(this.clientId >= 0 || clientId < 0) return false;
        this.clientId = clientId;
        return true;
    }

    /**
     * Sets or updates the http endpoint that 
     * the actuator should send events to
     * 
     * @param serverIP the IP address of the endpoint
     * @param serverPort the port number of the endpoint
     */
    public synchronized void setEndpoint(String serverIP, int serverPort){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Sets the frequency of event generation
     *
     * @param frequency the frequency of event generation in Hz (1/s)
     */
    public void setEventGenerationFrequency(double frequency){
        if(frequency >= 0) {
            this.eventGenerationFrequency = frequency;
        }
    }

    /**
     * Sends an Event object over a socket to a server.
     *
     * @param event The Event object to be sent.
     * @throws IOException If an I/O error occurs while sending the event.
     */
    public void sendEvent(Event event) throws IOException {
        String currentIP;
        int currentPort;
        synchronized (this) {
            currentIP = serverIP;
            currentPort = serverPort;
        }

        Socket sendSocket = new Socket(currentIP, currentPort);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()));
        out.println("actuator," + clientId + "," + port);
        out.flush();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(sendSocket.getOutputStream());
        objectOutputStream.writeObject(event);
        objectOutputStream.flush();
    }


    /**
     * Processes server commands related to the actuator.
     * The method processes commands to control the state of the actuator.
     *
     * @param command The server command related to the actuator (enum with additional required data).
     */
    public synchronized void processServerMessage(Request command) {
        // This method makes 0 sense. It takes a request object but it's suppose to take a SeverCommandToActuator enum (well obviously it needs more than just the enum)
        if(command.getRequestCommand() == RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE) {
            state = !state;
        } else if (command.getRequestCommand() == RequestCommand.CONTROL_SET_ACTUATOR_STATE) {
            String data = command.getRequestData();
            if(data.equals("true")) {
                state = true;
            } else {
                state = false;
            }
        }
    }

    @Override
    public String toString() {
        return "Actuator{" +
                "getId=" + getId() +
                ",ClientId=" + getClientId() +
                ",EntityType=" + getType() +
                ",IP=" + getIP() +
                ",Port=" + getPort() +
                '}';
    }

    // you will most likely need additional helper methods for this class

    /**
     * Generates an event based on the actuator type.
     * The method generates events for Switch-type actuators with boolean sensor values.
     *
     * @return An Event object representing sensor data for Switch-type actuators.
     * @throws IllegalArgumentException If the actuator type is not "Switch".
     */
    private Event generateEvent() {
        boolean sensorValue;
        if(this.type.equals("Switch")) {
            sensorValue = randomNumber.nextBoolean();
            synchronized (this) {
                this.state = sensorValue;
            }
        } else {
            throw new IllegalArgumentException("Actuator Type must be Switch");
        }
        int currentClientID;
        synchronized (this) {
            currentClientID = this.clientId;
        }
        return new ActuatorEvent(System.currentTimeMillis(), currentClientID, this.id, this.type, sensorValue);
    }

    /**
     * Starts the server thread to handle incoming connections and process requests.
     * Additionally, initializes a separate thread for sending events to the server.
     * This method continuously listens for incoming connections and processes received requests.
     * It also manages sending events to the server on a separate thread.
     */
    public void run() {

        port = portOffset.getAndIncrement();
        boolean failed = true;
        while(failed) {
            try {
                serverSocket = new ServerSocket(port);
                failed = false;
            } catch (IOException e) {
                port = portOffset.getAndIncrement();
            }
        }

        // Needs a thread to send data to the server.

        Thread output = new Thread( () -> {
            String currentIP;
            int currentPort;
            synchronized (this) {
                currentIP = serverIP;
                currentPort = serverPort;
            }
//            System.out.println("got current IPs");
//            System.out.println("Checking they are valid");
            // If endpoint not yet set, wait until it's set. Check every 5 seconds
            while(currentIP == null || currentPort == 0) {
//                System.out.println("Still checking they are valid");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (this) {
                    currentIP = serverIP;
                    currentPort = serverPort;
                }
            }
//            System.out.println("IP is valid");

            // Send data back to the server
            int timesFailed = 0;
            int eventsSent = 0;
            while(true) {
//                System.out.println("Events Sent: " + eventsSent++);
//                System.out.println("Times Failed: " + timesFailed);
                //Create a new event
                Event actuatorEvent = generateEvent();
                // Start sending events
                try {
                    sendEvent(actuatorEvent);
                    timesFailed = 0;
                } catch (IOException e) {
                    // Failed to send.
                    timesFailed++;
                }

                double currentEventGenFreq;
                synchronized (this) {
                    currentEventGenFreq = this.eventGenerationFrequency;
                }
                int waitTime;
                if(timesFailed >= 5) {
                    waitTime = 10000;
                } else {
                    waitTime = (int) (1/currentEventGenFreq * 1000);
                }
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        output.start();

        //
        try {
            while(true) {
//                System.out.println("Opening a serversocket and waiting for connections");
                Socket incomingSocket = serverSocket.accept();
//                System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());

                ObjectInputStream ois = new ObjectInputStream(incomingSocket.getInputStream());
                Object hopefullyARequest = ois.readObject();
                if (hopefullyARequest instanceof Request aRequest) {
                    this.processServerMessage(aRequest);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}