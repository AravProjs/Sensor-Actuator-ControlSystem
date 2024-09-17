package cpen221.mp3.event;

import java.io.Serial;
import java.io.Serializable;

public class SensorEvent implements Event, Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private double timeStamp;
    private int clientId;
    private int entityId;
    private String entityType;
    private double value;

    public SensorEvent(double TimeStamp,
                        int ClientId,
                        int EntityId, 
                        String EntityType, 
                        double Value) {
        this.timeStamp = TimeStamp;
        this.clientId = ClientId;
        this.entityId = EntityId;
        this.entityType = EntityType;
        this.value = Value;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    public int getClientId() {
        return clientId;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public double getValueDouble() {
        return value;
    }

    // Sensor events do not have a boolean value
    // no need to implement this method
    public boolean getValueBoolean() {
        return false;
    }

    @Override
    public String toString() {
        return "SensorEvent{" +
               "TimeStamp=" + getTimeStamp() +
               ",ClientId=" + getClientId() + 
               ",EntityId=" + getEntityId() +
               ",EntityType=" + getEntityType() + 
               ",Value=" + getValueDouble() + 
               '}';
    }
}
