package cpen221.mp3.client;

import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Entity;
import cpen221.mp3.entity.Sensor;

import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleClientTests{

    @Test
    public void testRegisterEntities() {
        Client client = new Client(0, "test@test.com", "127.0.0.1", 4578);

        Entity thermostat = new Sensor(0, client.getClientId(), "TempSensor");
        Entity valve = new Actuator(0, -1, "Switch", false);

        assertFalse(thermostat.registerForClient(1));   // thermostat is already registered to client 0
        assertTrue(thermostat.registerForClient(0));    // registering thermostat for existing client (client 0) is fine and should return true
        assertTrue(valve.registerForClient(1));         // valve was unregistered, and can be registered to client 1, even if it does not exist

//        System.out.println("This test finished");

//        thermostat.setEndpoint("127.0.0.1", 4578);

//        Thread thermoThread = new Thread(thermostat);
//        thermoThread.start();
//        try {
//            thermoThread.join();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Test
    public void testAddEntities() {
        Client client1 = new Client(0, "test1@test.com", "127.0.0.1", 4578);
        Client client2 = new Client(1, "test2@test.com", "127.0.0.1", 4578);

        Entity valve = new Actuator(0, -1, "Switch", false);

        assertTrue(client1.addEntity(valve));
        assertFalse(client2.addEntity(valve));

//        System.out.println("This test finished");

//        valve.setEndpoint("127.0.0.1", 4578);
//
//        Thread valveThread = new Thread(valve);
//        valveThread.start();
//
//        try {
//            valveThread.join();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Test
    public void testRunEntities() {
        Client client1 = new Client(0, "test1@test.com", "127.0.0.1", 4578);

        Entity thermostat = new Sensor(0, -1, "TempSensor");
        Entity valve = new Actuator(0, -1, "Switch", false);

        client1.addEntity(thermostat);
        client1.addEntity(valve);
    }

    @Test
    public void testSensorCreation() {
        int id = 1;
        String type = "TempSensor";

        Sensor sensor = new Sensor(id, type);

        assertNotNull(sensor);
        assertEquals(id, sensor.getId());
        assertEquals(type, sensor.getType());
        assertEquals(-1, sensor.getClientId());
    }

    @Test
    public void testSendEventThrowsIOException() {

        Sensor sensor = new Sensor(1, "TempSensor");
        Event event = new SensorEvent(123, 1, 1, "TempSensor", 25.0);

        assertThrows(IOException.class, () -> sensor.sendEvent(event));
    }

    @Test
    void testActuatorInitialization() {
        Actuator actuator = new Actuator(1, "Switch", true);
        assertNotNull(actuator);
        assertEquals(1, actuator.getId());
        assertEquals("Switch", actuator.getType());
        assertTrue(actuator.getState());
        assertEquals(-1, actuator.getClientId()); // Unregistered by default
    }

    @Test
    void testSetAndGetState() {
        Actuator actuator = new Actuator(1, "Switch", true);
        assertTrue(actuator.getState());
        actuator.updateState(false);
        assertFalse(actuator.getState());
    }

    @Test
    void testRegisterForClient() {
        Actuator actuator = new Actuator(1, "Switch", true);
        assertTrue(actuator.registerForClient(5));
        assertEquals(5, actuator.getClientId());
        assertFalse(actuator.registerForClient(10)); // Already registered
        assertEquals(5, actuator.getClientId());
    }




}