# 🌟 Sensor-Actuator Control System 🌟

The **Sensor-Actuator Control System** is a robust Java-based framework designed for real-time data processing and dynamic control in distributed sensor-actuator networks. It is ideal for applications such as robotics, IoT, and industrial automation. This system leverages an event-driven architecture to enable dynamic and responsive management of sensor data and actuator actions.

## 📚 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Class Descriptions](#class-descriptions)
- [Test Data](#test-data)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## 📖 Overview

The Sensor-Actuator Control System is built to handle real-time sensor data and manage actuator responses dynamically. It uses a multi-threaded messaging system for efficient resource management and high responsiveness, making it perfect for scenarios requiring real-time data processing and control.

## ✨ Features

- **Event-Driven Architecture**: Real-time data processing and dynamic control of sensors and actuators.
- **Multi-Threaded Messaging System**: Concurrent processing to handle multiple sensors and actuators efficiently.
- **Dynamic Data Filtering**: Apply filters on incoming sensor data for precise control and system stability.
- **Modular Design**: Easily extendable to support various types of sensors, actuators, and control logic.
- **Comprehensive Test Coverage**: High code coverage across client, server, and event modules ensures robust performance and reliability.

## ⚙️ Installation

To set up and run the project on your local machine, follow these steps:

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/AravProjs/Sensor-Actuator-ControlSystem.git
   cd Sensor-Actuator-ControlSystem
   ```
2. **Build the Project:** Ensure you have Java (JDK 11 or higher) and a build tool like Gradle installed. Then, run:
   ```bash
   ./gradlew build
   ```
3. **Run Tests:** To verify the installation and test the functionality:
   ```bash
   ./gradlew test
   ```

## 🚀 Usage

1. **Start the System:** Launch the application with:
   ```bash
   java -jar build/libs/Sensor-Actuator-ControlSystem.jar
   ```
2. **Configure Sensor and Actuator Parameters:** Edit the configuration file `config/settings.json` to set parameters for sensors and actuators (e.g., types, frequencies, endpoints).
3. **Monitor Real-Time Data:** The system logs real-time data to the `logs` directory, which can be monitored for system activity and performance.

## 🏗️ Project Structure

```
Sensor-Actuator-ControlSystem/
├── client/
│   ├── Client.java                         # Client-side logic for interacting with the server
│   ├── Request.java                        # Defines the structure of requests sent by clients
│   ├── RequestCommand.java                 # Enumerates types of commands a request can have
│   └── RequestType.java                    # Enumerates different request categories
├── entity/
│   ├── Actuator.java                       # Represents an actuator entity
│   ├── Entity.java                         # Base interface for all entities (sensors and actuators)
│   └── Sensor.java                         # Represents a sensor entity
├── event/
│   ├── ActuatorEvent.java                  # Event structure for actuator-related activities
│   ├── Event.java                          # Interface for events handled by the system
│   ├── RequestOrEvent.java                 # Enum to distinguish between requests and events
│   ├── SensorEvent.java                    # Event structure for sensor-related activities
│   └── TimeToProcess.java                  # Manages event processing timing
├── handler/
│   ├── MessageHandler.java                 # Manages incoming messages from clients and entities
│   └── MessageHandlerThread.java           # Thread class to handle messages concurrently
├── server/
│   ├── Filter.java                         # Implements dynamic filtering logic for events
│   ├── FilterException.java                # Custom exception class for filter-related errors
│   ├── Server.java                         # Main server class to handle events and manage client interactions
│   ├── SeverCommandToActuator.java         # Enum representing server commands directed at actuators
│   └── TimeWindow.java                     # Represents a time window for event filtering
├── data/
│   └── tests/                              # Test data for validating system functionality
│       ├── single_client_1000_events_in-order.csv       # Events in chronological order
│       └── single_client_1000_events_out-of-order.csv   # Events out of order
├── README.md                               # Project documentation
├── build.gradle                            # Gradle build configuration file
└── config/
    └── settings.json                       # Configuration file for sensors and actuators
```

## 📝 Class Descriptions

### Client Module:
- **Client.java:** Manages client-side functionality, including registering entities, sending requests to the server, and processing server responses. Supports concurrent management of multiple entities (sensors and actuators).
- **Request.java, RequestCommand.java, RequestType.java:** Define the structure, commands, and types of requests that clients can send to the server. These requests can be used for configuration, control, analysis, and prediction purposes.

### Entity Module:
- **Entity.java:** Interface defining common methods for all entities (sensors and actuators).
- **Actuator.java:** Represents an actuator entity, capable of receiving commands from the server, generating events, and adjusting its state accordingly.
- **Sensor.java:** Represents a sensor entity, capable of generating data events and sending them to the server for processing.

### Event Module:
- **Event.java:** Interface defining common methods for events generated by entities.
- **ActuatorEvent.java, SensorEvent.java:** Implementations of the Event interface for actuator and sensor activities, respectively. Each event contains details such as timestamps, entity types, and values.
- **RequestOrEvent.java:** Enum to differentiate between requests and events.
- **TimeToProcess.java:** Represents an event or request scheduled for processing at a specific time.

### Handler Module:
- **MessageHandler.java:** Manages incoming messages from clients and entities, queues them for processing, and delegates tasks to MessageHandlerThread.
- **MessageHandlerThread.java:** Thread class that processes each incoming message in a separate thread to ensure concurrency.

### Server Module:
- **Server.java:** Core server class responsible for handling incoming events and requests, applying filters, managing actuator states, and predicting future values. It maintains a list of entities, events, and active connections.
- **Filter.java, FilterException.java:** Implements filtering logic to dynamically control the flow and processing of events. FilterException handles errors related to filtering operations.
- **SeverCommandToActuator.java:** Enum representing different commands that the server can send to actuators.
- **TimeWindow.java:** Represents a time window for event filtering, allowing the server to process events within specific time ranges.

## 🧪 Test Data

The `data/tests/` directory contains CSV files used to validate the system's functionality:

- **single_client_1000_events_in-order.csv:** Contains 1000 events for a single client in chronological order. This file is used to test the system's ability to handle events that arrive in the correct sequence.
- **single_client_1000_events_out-of-order.csv:** Contains 1000 events for a single client, with events not in chronological order. This file tests the system's capability to process events that arrive out of sequence.

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes with clear messages.
4. Submit a pull request with a description of your changes.

## 📄 License

This project is licensed under the MIT License. See the LICENSE file for more details.

## 📬 Contact

For any questions or support, please open an issue on the GitHub repository or contact the project maintainers.
