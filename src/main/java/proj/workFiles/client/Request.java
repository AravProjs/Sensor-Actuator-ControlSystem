package cpen221.mp3.client;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 3L;

    private final double timeStamp;
    private final RequestType requestType;
    private final RequestCommand requestCommand;
    private final String requestData;

    public Request(RequestType requestType, RequestCommand requestCommand, String requestData) {
        this.timeStamp = System.currentTimeMillis();
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public RequestCommand getRequestCommand() {
        return requestCommand;
    }

    public String getRequestData() {
        return requestData;
    }
}