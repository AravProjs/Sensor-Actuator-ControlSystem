package cpen221.mp3.event;

import java.util.List;

import cpen221.mp3.CSVEventReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventTests{

    String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();

    @Test
    public void testCreateSingleEvent() {
        Event event = new SensorEvent(0.000111818, 0, 1,"TempSensor", 1.0);
        assertEquals(0.000111818, event.getTimeStamp());
        assertEquals(0, event.getClientId());
        assertEquals(1, event.getEntityId());
        assertEquals("TempSensor", event.getEntityType());
        assertEquals(1.0, event.getValueDouble());
    }

    @Test
    public void testSensorEvent() {
        Event sensorEvent = eventList.get(0);
        assertEquals(0.00011181831359863281, sensorEvent.getTimeStamp());
        assertEquals(0, sensorEvent.getClientId());
        assertEquals(0, sensorEvent.getEntityId());
        assertEquals("TempSensor", sensorEvent.getEntityType());
        assertEquals(22.21892397393261, sensorEvent.getValueDouble());
    }

    @Test
    public void testActuatorEvent() {
        Event actuatorEvent = eventList.get(3);
        assertEquals(0.33080601692199707, actuatorEvent.getTimeStamp());
        assertEquals(0, actuatorEvent.getClientId());
        assertEquals(97, actuatorEvent.getEntityId());
        assertEquals("Switch", actuatorEvent.getEntityType());
        assertEquals(false, actuatorEvent.getValueBoolean());
    }

    @Test
    public void testCreateActuatorEvent() {
        Event event = new ActuatorEvent(0.123456789, 1, 42, "Fan", true);
        assertEquals(0.123456789, event.getTimeStamp());
        assertEquals(1, event.getClientId());
        assertEquals(42, event.getEntityId());
        assertEquals("Fan", event.getEntityType());
        assertEquals(true, event.getValueBoolean());
    }

    @Test
    public void testActuatorEventToString() {
        Event event = new ActuatorEvent(0.987654321, 5, 10, "Light", false);
        String expected = "ActuatorEvent{TimeStamp=0.987654321,ClientId=5,EntityId=10,EntityType=Light,Value=false}";
        assertEquals(expected, event.toString());
    }
        @Test
        public void testCreateSingleSensorEvent() {
            Event event = new SensorEvent(0.123456789, 1, 42, "Temperature", 25.5);
            assertEquals(0.123456789, event.getTimeStamp());
            assertEquals(1, event.getClientId());
            assertEquals(42, event.getEntityId());
            assertEquals("Temperature", event.getEntityType());
            assertEquals(25.5, event.getValueDouble());
        }

        @Test
        public void testSensorEventToString() {
            Event event = new SensorEvent(0.987654321, 5, 10, "Humidity", 80.0);
            String expected = "SensorEvent{TimeStamp=0.987654321,ClientId=5,EntityId=10,EntityType=Humidity,Value=80.0}";
            assertEquals(expected, event.toString());
        }

    }
