package cpen221.mp3.event;

import cpen221.mp3.client.Request;

import java.net.Socket;

public class TimeToProcess {

    private Event event;
    private Request request;
    private double timeAtWhichToProcess;
    private RequestOrEvent requestOrEvent;
    private int clientID;
    private Socket clientSocket;

    public TimeToProcess(double timeAtWhichToProcess, Event event) {
        this.timeAtWhichToProcess = timeAtWhichToProcess;
        this.event = event;
        this.requestOrEvent = RequestOrEvent.EVENT;
    }

    public TimeToProcess(double timeAtWhichToProcess, Request request, int clientID, Socket clientSocket) {
        this.timeAtWhichToProcess = timeAtWhichToProcess;
        this.request = request;
        this.requestOrEvent = RequestOrEvent.REQUEST;
        this.clientID = clientID;
        this.clientSocket = clientSocket;
    }

    public double getTimeStamp() {
        if(requestOrEvent == RequestOrEvent.REQUEST) {
            return request.getTimeStamp();
        } else {
            return event.getTimeStamp();
        }
    }

    public double getClientId() {
        if(requestOrEvent == RequestOrEvent.REQUEST) {
            return clientID;
        } else {
            return event.getClientId();
        }
    }

    public Event getOriginalEvent() {
        return event;
    }

    public Request getOriginalRequest() {
        return request;
    }

    public RequestOrEvent getType() {
        return requestOrEvent;
    }

    public synchronized double getTimeAtWhichToProcess() {
        return timeAtWhichToProcess;
    }

    public synchronized void setTimeAtWhichToProcess(double time) {
        timeAtWhichToProcess = time;
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }
}
