package cpen221.mp3.server;

import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.client.Client;
import cpen221.mp3.event.Event;
import cpen221.mp3.client.Request;
import cpen221.mp3.event.RequestOrEvent;
import cpen221.mp3.event.TimeToProcess;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;

public class Server implements Runnable {
    private final Client client;
    private final Object waitTimeLock = new Object();
    private double maxWaitTime = 2; // in seconds

    // you may need to add additional private fields
    private final BlockingQueue<TimeToProcess> tasksToDo = new LinkedBlockingQueue<>();
    private final Set<Integer> entitySet = new HashSet<>();
    public final Map<Integer, Integer> actuator_port_map = new HashMap<>();
    public final Map<Integer, String> actuator_IP_map = new HashMap<>();
    private final Map<Integer, Integer> entity_activity = new HashMap<>();
    private final List<Event> eventsList = new ArrayList<>();
    private final List<Event> logList = new ArrayList<>();
    private final Map<Integer, Filter> filterToActuatorToggleMap = new HashMap<>();
    private final Map<Integer, Filter> filterToActuatorSetMap = new HashMap<>();
    private final Object logFilterLock = new Object();
    private Filter logFilter;
    private Socket clientConnectionSocket;

    public Server(Client client) {
        this.client = client;
    }

    public int getClientID() {
        return client.getClientId();
    }

    /**
     * Update the max wait time for the client.
     * The max wait time is the maximum amount of time
     * that the server can wait for before starting to process each event of the client:
     * It is the difference between the time the message was received on the server
     * (not the event timeStamp from above) and the time it started to be processed.
     *
     * @param maxWaitTime the new max wait time
     */
    public void updateMaxWaitTime(double maxWaitTime) {
        synchronized (waitTimeLock) {
            this.maxWaitTime = maxWaitTime;
        }
        // implement this method

        // Important note: updating maxWaitTime may not be as simple as
        // just updating the field. You may need to do some additional
        // work to ensure that events currently being processed are not
        // dropped or ignored by the change in maxWaitTime.
    }

    public double getMaxWaitTime() {
        double newMaxWait;
        synchronized (waitTimeLock) {
            newMaxWait = this.maxWaitTime;
        }
        return newMaxWait;
    }

    /**
     * Set the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event 
     * that was received by the server the latest.
     * <p>
     * If the actuator is not registered for the client, then this method should do nothing.
     * 
     * @param filter the filter to check
     * @param actuator the actuator to set the state of as true
     */
    public void setActuatorStateIf(Filter filter, Actuator actuator) {
        // implement this method and send the appropriate SeverCommandToActuator as a Request to the actuator
        synchronized (filterToActuatorSetMap) {
            filterToActuatorSetMap.put(actuator.getId(), filter);
        }
    }
    
    /**
     * Toggle the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event 
     * that was received by the server the latest.
     * <p>
     * If the actuator has never sent an event to the server, then this method should do nothing.
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter the filter to check
     * @param actuator the actuator to toggle the state of (true -> false, false -> true)
     */
    public void toggleActuatorStateIf(Filter filter, Actuator actuator) {
        // implement this method and send the appropriate SeverCommandToActuator as a Request to the actuator
        synchronized (filterToActuatorToggleMap) {
            filterToActuatorToggleMap.put(actuator.getId(), filter);
        }
    }

    /**
     * Log the event ID for which a given filter was satisfied.
     * This method is checked for every event received by the server.
     *
     * @param filter the filter to check
     */
    public void logIf(Filter filter) {
        synchronized (logFilterLock) {
            logFilter = filter;
        }
    }

    /**
     * Return all the logs made by the "logIf" method so far.
     * If no logs have been made, then this method should return an empty list.
     * The list should be sorted in the order of event timestamps.
     * After the logs are read, they should be cleared from the server.
     *
     * @return list of event IDs 
     */
    public List<Integer> readLogs() {
        logList.sort(Comparator.comparingDouble(Event::getTimeStamp));
        List<Integer> returnList = new ArrayList<>(logList.stream().map(Event::getEntityId).toList());
        logList.clear();
        return returnList;
    }

    /**
     * List all the events of the client that occurred in the given time window.
     * Here the timestamp of an event is the time at which the event occurred, not 
     * the time at which the event was received by the server.
     * If no events occurred in the given time window, then this method should return an empty list.
     *
     * @param timeWindow the time window of events, inclusive of the start and end times
     * @return list of the events for the client in the given time window
     */
    public List<Event> eventsInTimeWindow(TimeWindow timeWindow) {
        double start = timeWindow.startTime;
        double end = timeWindow.endTime;

        // Use a linkedList because we could be adding hundreds of events and rebuilding an arraylist each time sucks.
        List<Event> eventsInWindow = new LinkedList<>();

        for(Event event : eventsList) {
            if(event.getTimeStamp() >= start && event.getTimeStamp() <= end) {
                eventsInWindow.add(event);
            }
        }

        // ArrayLists are more useful for retrieving data, which after this point is the more likely operation.
        eventsInWindow = new ArrayList<>(eventsInWindow);

        return eventsInWindow;
    }

     /**
     * Returns a set of IDs for all the entities of the client for which
     * we have received events so far.
     * Returns an empty list if no events have been received for the client.
     * 
     * @return list of all the entities of the client for which we have received events so far
     */
    public List<Integer> getAllEntities() {
        return new ArrayList<>(entitySet);
    }

    /**
     * List the latest n events of the client.
     * Here the order is based on the original timestamp of the events, not the time at which the events were received by the server.
     * If the client has fewer than n events, then this method should return all the events of the client.
     * If no events exist for the client, then this method should return an empty list.
     * If there are multiple events with the same timestamp in the boundary,
     * the ones with largest EntityId should be included in the list.
     *
     * @param n the max number of events to list
     * @return list of the latest n events of the client
     */
    public List<Event> lastNEvents(int n) {
        List<Event> returnList;
        synchronized (eventsList) {
            if(n > eventsList.size()) {
                n = eventsList.size();
            }
            returnList = new ArrayList<>(eventsList.subList(eventsList.size() - n, eventsList.size()));
        }

        return returnList;
    }

    /**
     * returns the ID corresponding to the most active entity of the client
     * in terms of the number of events it has generated.
     * <p>
     * If there was a tie, then this method should return the largest ID.
     * 
     * @return the most active entity ID of the client
     */
    public int mostActiveEntity() {
        AtomicInteger maxTimes = new AtomicInteger();
        AtomicInteger mostActive = new AtomicInteger(-1);
        synchronized (entity_activity) {
            entity_activity.forEach((x, y) -> {
                if (y > maxTimes.get()) {
                    maxTimes.set(y);
                    mostActive.set(x);
                } else if (y == maxTimes.get() && x > mostActive.get()) {
                    mostActive.set(x);
                }
            });
        }
        return mostActive.get();
    }

    /**
     * the client can ask the server to predict what will be 
     * the next n timestamps for the next n events 
     * of the given entity of the client (the entity is identified by its ID).
     * <p>
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     * 
     * @param entityId the ID of the entity
     * @param n the number of timestamps to predict
     * @return list of the predicted timestamps
     */
    public List<Double> predictNextNTimeStamps(int entityId, int n) {
        // implement this method
        return null;
    }

    /**
     * the client can ask the server to predict what will be 
     * the next n values of the timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     * The values correspond to Event.getValueDouble() or Event.getValueBoolean() 
     * based on the type of the entity. That is why the return type is List<Object>.
     * <p>
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     * 
     * @param entityId the ID of the entity
     * @param n the number of double value to predict
     * @return list of the predicted timestamps
     */
    public List<Object> predictNextNValues(int entityId, int n) {
        return null;
    }

    public void processIncomingEvent(Event event) {

        int eventEntityID = event.getEntityId();

        if(entitySet.add(eventEntityID)) {
            entity_activity.put(eventEntityID, 1);
        } else {
            entity_activity.put(eventEntityID, entity_activity.get(eventEntityID) + 1);
        }
        // If the event is too late, then we are choosing to drop it entirely. Not dropping it could cause some weirdness (ie, a switch changing state that we want in the opposite state).
        // We choose to log a dropped log under entity activity because it does represent an event that an entity did send, even though it arrived too late.
        synchronized (eventsList) {
            if(eventsList.isEmpty() || event.getTimeStamp() > eventsList.get(eventsList.size()-1).getTimeStamp()) {
                eventsList.add(event);
            } else {
                return;
            }
        }
        // Process events here

        synchronized (logFilterLock) {
            synchronized (logList) {
                if(logFilter != null && logFilter.satisfies(event)) {
                    logList.add(event);
                }
            }
        }

        synchronized (filterToActuatorToggleMap) {
            filterToActuatorToggleMap.forEach( (x,y) -> {
                if(y.satisfies(event)) {
                    Socket respondSocket;
                    int actuatorPort;
                    String actuatorIP;
                    Request response = new Request(RequestType.CONTROL, RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE, "");
                    synchronized (actuator_port_map) {
                        synchronized (actuator_IP_map) {
                            actuatorPort = actuator_port_map.get(x);
                            actuatorIP = actuator_IP_map.get(x);
                        }
                    }
                    try {
                        respondSocket = new Socket( actuatorIP, actuatorPort);
                        ObjectOutputStream oos = new ObjectOutputStream(respondSocket.getOutputStream());
                        oos.writeObject(response);
                        oos.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        synchronized (filterToActuatorSetMap) {
            filterToActuatorSetMap.forEach( (x,y) -> {
                if(y.satisfies(event)) {
                    Socket respondSocket;
                    int actuatorPort;
                    String actuatorIP;
                    Request response = new Request(RequestType.CONTROL, RequestCommand.CONTROL_SET_ACTUATOR_STATE, "true");
                    synchronized (actuator_port_map) {
                        synchronized (actuator_IP_map) {
                            actuatorPort = actuator_port_map.get(x);
                            actuatorIP = actuator_IP_map.get(x);
                        }
                    }
                    try {
                        respondSocket = new Socket( actuatorIP, actuatorPort);
                        ObjectOutputStream oos = new ObjectOutputStream(respondSocket.getOutputStream());
                        oos.writeObject(response);
                        oos.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public void processIncomingRequest(Request request) {
        if(request.getRequestType() == RequestType.CONFIG) {
            processRequestConfig(request);
        } else if (request.getRequestType() == RequestType.CONTROL) {
            processRequestControl(request);
        } else if (request.getRequestType() == RequestType.ANALYSIS) {
            processRequestAnalysis(request);
        } else if (request.getRequestType() == RequestType.PREDICT) {
            processRequestPredict(request);
        }
    }

    public void processIncomingEventOrRequest(TimeToProcess eventOrRequest) {
        tasksToDo.add(eventOrRequest);
    }

    public void run() {
        while(true) {
            TimeToProcess requestOrEvent;
            try {
                requestOrEvent = tasksToDo.take();
            } catch (InterruptedException e) {
                continue;
            }

            if(requestOrEvent.getType() == RequestOrEvent.EVENT) {
                processIncomingEvent(requestOrEvent.getOriginalEvent());
            } else {
                this.clientConnectionSocket = requestOrEvent.getClientSocket();
                processIncomingRequest(requestOrEvent.getOriginalRequest());
            }
        }
    }

    private Filter parseFilter(String filterString) throws FilterException {
        if(filterString.matches(".*BooleanOperator.*")) {
            Pattern pattern = Pattern.compile("Filter\\{BooleanOperator=(.*), BooleanValue=(.*)}");
            Matcher matcher = pattern.matcher(filterString);
            if(!matcher.find()) throw new FilterException();
            return new Filter(BooleanOperator.valueOf(matcher.group(0)), Boolean.parseBoolean(matcher.group(1)));
        } else if (filterString.matches(".*DoubleField")) {
            Pattern pattern = Pattern.compile("Filter\\{DoubleField=(.*), DoubleOperator=(.*), DoubleValue=(.*)}");
            Matcher matcher = pattern.matcher(filterString);
            if(!matcher.find()) throw new FilterException();
            return new Filter(matcher.group(0), DoubleOperator.valueOf(matcher.group(1)), Double.parseDouble(matcher.group(2)));
        } else if (filterString.matches(".*ComposedFilters.*")) {
            Pattern pattern = Pattern.compile("Filter\\{ComposedFilters=(.*)}");
            Matcher matcher = pattern.matcher(filterString);
            if(!matcher.find()) throw new FilterException();
            String listString = matcher.group(0);
            listString = listString.substring(1, listString.length() - 1);

            Pattern pattern1 = Pattern.compile("Filter\\{ComposedFilters=\\[.*\\]}|Filter\\{(.+?)}");
            Matcher matcher1 = pattern1.matcher(listString);
            List<Filter> filterList = new ArrayList<>();
            while(matcher1.find()) {
                filterList.add(parseFilter(matcher1.group(0)));
            }
            return new Filter(filterList);
        }
        throw new FilterException();
    }

    private void processRequestConfig(Request request) {
        if(request.getRequestCommand() == RequestCommand.CONFIG_UPDATE_MAX_WAIT_TIME) {
            updateMaxWaitTime( Double.parseDouble(request.getRequestData()));
        }
    }

    private void processRequestControl(Request request) {
        String data = request.getRequestData();
        if(request.getRequestCommand() == RequestCommand.CONTROL_NOTIFY_IF) {
            Filter newFilter = null;
            try {
                newFilter = parseFilter(data);
            } catch (FilterException e) {
                return;
            }
            logIf(newFilter);
        } else if (request.getRequestCommand() == RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE) {
            // The only thing needed is actuator ID, so only the actuator ID should be sent. This should be of the form <id>,<filter>
            String[] getID = data.split(",");
            String id = getID[0];
            String filterString = data.substring(id.length()+1);
            Filter newFilter;
            try {
                newFilter = parseFilter(filterString);
            } catch (FilterException e) {
                return;
            }
            toggleActuatorStateIf(newFilter, new Actuator(Integer.parseInt(id), "switch", true));
        } else if (request.getRequestCommand() == RequestCommand.CONTROL_SET_ACTUATOR_STATE) {
            // The only thing needed is actuator ID, so only the actuator ID should be sent. This should be of the form <id>,<filter>
            String[] getID = data.split(",");
            String id = getID[0];
            String filterString = data.substring(id.length()+1);
            Filter newFilter;
            try {
                newFilter = parseFilter(filterString);
            } catch (FilterException e) {
                return;
            }
            setActuatorStateIf(newFilter, new Actuator(Integer.parseInt(id), "switch", true));
        }
    }

    private void processRequestAnalysis(Request request) {
        if(request.getRequestCommand() == RequestCommand.ANALYSIS_GET_ALL_ENTITIES) {
            List<Integer> entityList = getAllEntities();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                oos.writeObject(entityList);
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (request.getRequestCommand() == RequestCommand.ANALYSIS_GET_LATEST_EVENTS) {
            int numberOfEvents = Integer.parseInt(request.getRequestData());
            List<Event> eventsList = lastNEvents(numberOfEvents);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                oos.writeObject(eventsList);
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (request.getRequestCommand() == RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW) {
            String data = request.getRequestData();

            Pattern pattern = Pattern.compile("Filter\\{BooleanOperator=(.*), BooleanValue=(.*)}");
            Matcher matcher = pattern.matcher(data);
            if(!matcher.find()) return;
            TimeWindow timeWindow = new TimeWindow(Double.parseDouble(matcher.group(0)), Double.parseDouble(matcher.group(1)));

            List<Event> eventsList = eventsInTimeWindow(timeWindow);

            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                oos.writeObject(eventsList);
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (request.getRequestCommand() == RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY) {
            Integer mostActiveEntityObject = mostActiveEntity();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                oos.writeObject(mostActiveEntityObject);
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (request.getRequestCommand() == RequestCommand.ANALYSIS_GET_LOGS) {
            List<Integer> logList = readLogs();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                oos.writeObject(logList);
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processRequestPredict(Request request) {

    }


}
