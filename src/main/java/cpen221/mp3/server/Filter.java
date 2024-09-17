package cpen221.mp3.server;

import cpen221.mp3.event.Event;

import java.util.ArrayList;
import java.util.List;
import cpen221.mp3.event.ActuatorEvent;

enum DoubleOperator {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}

enum BooleanOperator {
    EQUALS,
    NOT_EQUALS
}

public class Filter {
    // you can add private fields and methods to this class

    /**
     * Constructs a filter that compares the boolean (actuator) event value
     * to the given boolean value using the given BooleanOperator.
     * (X (BooleanOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A BooleanOperator can be one of the following:
     *
     * BooleanOperator.EQUALS
     * BooleanOperator.NOT_EQUALS
     *
     * @param operator the BooleanOperator to use to compare the event value with the given value
     * @param value the boolean value to match
     */
    private BooleanOperator booleanOperator;
    private boolean booleanValue;
    private String doubleField;
    private DoubleOperator doubleOperator;
    private double doubleValue;
    private List<Filter> composedFilters;

    public Filter(BooleanOperator operator, boolean value) {
        this.booleanOperator = operator;
        this.booleanValue = value;
    }

    /**
     * Constructs a filter that compares a double field in events
     * with the given double value using the given DoubleOperator.
     * (X (DoubleOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A DoubleOperator can be one of the following:
     *
     * DoubleOperator.EQUALS
     * DoubleOperator.GREATER_THAN
     * DoubleOperator.LESS_THAN
     * DoubleOperator.GREATER_THAN_OR_EQUALS
     * DoubleOperator.LESS_THAN_OR_EQUALS
     *
     * For non-double (boolean) value events, the satisfies method should return false.
     *
     * @param field the field to match (event "value" or event "timestamp")
     * @param operator the DoubleOperator to use to compare the event value with the given value
     * @param value the double value to match
     *
     * @throws IllegalArgumentException if the given field is not "value" or "timestamp"
     */
    public Filter(String field, DoubleOperator operator, double value) {
        if (!field.equals("value") && !field.equals("timestamp")) {
            throw new IllegalArgumentException("Field must be 'value' or 'timestamp'");
        }
        this.doubleField = field;
        this.doubleOperator = operator;
        this.doubleValue = value;
    }

    /**
     * A filter can be composed of other filters.
     * in this case, the filter should satisfy all the filters in the list.
     * Constructs a complex filter composed of other filters.
     *
     * @param filters the list of filters to use in the composition
     */
    public Filter(List<Filter> filters) {
        this.composedFilters = filters;
    }

    /**
     * Returns true if the given event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(Event event) {
        if (booleanOperator != null) {
            boolean eventValue = event.getValueBoolean();
            if (booleanOperator == BooleanOperator.EQUALS) {
                return eventValue == booleanValue;
            } else {
                return eventValue != booleanValue;
            }

        } else if (composedFilters != null) {
            for (Filter filter : composedFilters) {
                if (!filter.satisfies(event)) {
                    return false;
                }
            }
            return true;

        } else if (doubleField != null) {
            double compareValue = doubleField.equals("value") ? event.getValueDouble() : event.getTimeStamp();
            switch (doubleOperator) {
                case EQUALS:
                    return compareValue == doubleValue;
                case GREATER_THAN:
                    return compareValue > doubleValue;
                case LESS_THAN:
                    return compareValue < doubleValue;
                case GREATER_THAN_OR_EQUALS:
                    return compareValue >= doubleValue;
                case LESS_THAN_OR_EQUALS:
                    return compareValue <= doubleValue;
            }
        }
        // Return true by default if none of the conditions above are met
        return true;
    }

    /**
     * Returns true if the given list of events satisfies the filter criteria.
     *
     * @param events the list of events to check
     * @return true if every event in the list satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(List<Event> events) {
        for (Event event : events) {
            if (!satisfies(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a new event if it satisfies the filter criteria.
     * If the given event does not satisfy the filter criteria, then this method should return null.
     *
     * @param event the event to sift
     * @return a new event if it satisfies the filter criteria, null otherwise
     */
    public Event sift(Event event) {
        if (satisfies(event)) {
            return event;
        }
        return null;
    }

    /**
     * Returns a list of events that contains only the events in the given list that satisfy the filter criteria.
     * If no events in the given list satisfy the filter criteria, then this method should return an empty list.
     *
     * @param events the list of events to sift
     * @return a list of events that contains only the events in the given list that satisfy the filter criteria
     *        or an empty list if no events in the given list satisfy the filter criteria
     */
    public List<Event> sift(List<Event> events) {
        List<Event> siftedEvents = new ArrayList<>();
        for (Event event : events) {
            if (satisfies(event)) {
                siftedEvents.add(event);
            }
        }
        return siftedEvents;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Filter{");
        if (booleanOperator != null) {
            sb.append("BooleanOperator=").append(booleanOperator);
            sb.append(", BooleanValue=").append(booleanValue);
        } else if (doubleField != null) {
            sb.append("DoubleField=").append(doubleField);
            sb.append(", DoubleOperator=").append(doubleOperator);
            sb.append(", DoubleValue=").append(doubleValue);
        } else if (composedFilters != null) {
            sb.append("ComposedFilters=").append(composedFilters);
        } else {
            sb.append("EmptyFilter");
        }
        sb.append("}");
        return sb.toString();
    }
}