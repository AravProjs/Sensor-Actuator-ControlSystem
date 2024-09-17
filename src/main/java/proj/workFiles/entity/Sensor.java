
package cpen221.mp3.entity;

import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class Sensor implements Entity, Runnable {
    private final int id;
    private int clientId;
    private final String type;
    private String serverIP = null;
    private int serverPort = 0;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)
    private Random randomNumber = new Random();

    /**
     * Constructs a Sensor object with an ID and sensor type. The client ID remains unregistered (-1).
     *
     * @param id   The ID of the sensor.
     * @param type The type of the sensor.
     */
    public Sensor(int id, String type) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
    }

    /**
     * Constructs a Sensor object with an ID, client ID, and sensor type.
     *
     * @param id        The ID of the sensor.
     * @param clientId  The client ID associated with the sensor.
     * @param type      The type of the sensor.
     */
    public Sensor(int id, int clientId, String type) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
    }

    /**
     * Constructs a Sensor object with an ID, sensor type, server IP, and server port.
     * The client ID remains unregistered (-1).
     *
     * @param id         The ID of the sensor.
     * @param type       The type of the sensor.
     * @param serverIP   The server IP address.
     * @param serverPort The server port number.
     */
    public Sensor(int id, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;   // remains unregistered
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Constructs a Sensor object with an ID, client ID, sensor type, server IP, and server port.
     *
     * @param id         The ID of the sensor.
     * @param clientId   The client ID associated with the sensor.
     * @param type       The type of the sensor.
     * @param serverIP   The server IP address.
     * @param serverPort The server port number.
     */
    public Sensor(int id, int clientId, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Gets the ID of the object.
     *
     * @return The ID of the object.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the client ID associated with the object.
     *
     * @return The client ID associated with the object.
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Gets the type of the object.
     *
     * @return The type of the object.
     */
    public String getType() {
        return type;
    }

    /**
     * Checks whether the object is an actuator.
     *
     * @return True if the object is an actuator, otherwise false.
     */
    public boolean isActuator() {
        return false;
    }

    /**
     * Registers the sensor for the given client
     *
     * @return true if the sensor is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public synchronized boolean registerForClient(int clientId) {
        if(this.clientId == clientId) return true;
        if(this.clientId >= 0 || clientId < 0) return false;
        this.clientId = clientId;
        return true;
    }

    /**
     * Sets or updates the http endpoint that
     * the sensor should send events to
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
     * Sends an Event object over the network.
     *
     * @param event The Event object to be sent.
     * @throws IOException If an I/O error occurs while sending the event.
     */
    public void sendEvent(Event event) throws IOException {
        // implement this method
        // note that Event is a complex object that you need to serialize before sending
        String currentIP;
        int currentPort;
        synchronized (this) {
            currentIP = serverIP;
            currentPort = serverPort;
        }

        Socket sendSocket = new Socket(currentIP, currentPort);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()));
        out.println("entity," + clientId);
        out.flush();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(sendSocket.getOutputStream());
        objectOutputStream.writeObject(event);
        objectOutputStream.flush();
    }

    /**
     * Generates an Event based on the type of sensor.
     * The sensor type determines the range and type of sensor value generated.
     *
     * @return An Event object representing sensor data.
     * @throws IllegalArgumentException If the sensor type is not one of TempSensor, PressureSensor, or CO2Sensor.
     */
    private Event generateEvent() {
        double sensorValue;
        if(this.type.equals("TempSensor")) {
            sensorValue = (randomNumber.nextDouble() * 4) + 20;
        } else if (this.type.equals("PressureSensor")) {
            sensorValue = (randomNumber.nextDouble() * 4) + 1020;
        } else if (this.type.equals("CO2Sensor")) {
            sensorValue = (randomNumber.nextDouble() * 50) + 400;
        } else {
            throw new IllegalArgumentException("Sensor Type must be of TempSensor, PressureSensor, or CO2Sensor");
        }
        int currentClientID;
        synchronized (this) {
            currentClientID = this.clientId;
        }
        return new SensorEvent(System.currentTimeMillis(), currentClientID, this.id, this.type, sensorValue);
    }


    /**
     * Continuously sends generated events to the server based on the configured event generation frequency.
     * Waits for the endpoint information and retries sending data in case of failures.
     *
     * @throws RuntimeException If an InterruptedException occurs during thread sleep.
     */
    public void run() {
        String currentIP;
        int currentPort;
        synchronized (this) {
            currentIP = serverIP;
            currentPort = serverPort;
        }
        // If endpoint not yet set, wait until it's set. Check every 5 seconds
        while(currentIP == null || currentPort == 0) {
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

        // Send data back to the server
        int timesFailed = 0;
        int eventsSent = 0;
        while(true) {
//            System.out.println("Events Sent: " + eventsSent++);
//            System.out.println("Times Failed: " + timesFailed);
            //Create a new event
            Event sensorEvent = generateEvent();
            // Start sending events
            try {
                sendEvent(sensorEvent);
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
            if(timesFailed == 5) {
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
    }
}