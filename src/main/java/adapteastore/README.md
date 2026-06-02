# Adapteastore

Adateastore works in communication with the Adaptable TeaStore, providing a way to adapt the cache size using a PID controller. 

The communication between the Adapteastore and the Adaptable TeaStore is done using the Adaptiflow framework, which has specific components for the communication between the Adaptable TeaStore and another service, in this case the Adapteastore. 
The framework is used in the PersistenceCollector and Bridge classes, letting the Adapteastore collect the information from the Adaptable TeaStore and send them to the PID controller.

## Usage

To provide the necessary information for the Adapteastore, the Adapatble TeaStore services need to be running. 
The Image and Persistence services are required for the Adapteastore to function properly.

Both services are modified from the original Adaptable TeaStore, the configuration files are located in the 'Sources/examples/docker/' directory, 
and the 'docker-compose_default.yaml' file has been updated for the Persistence and Image services to be running respectively on ports 8082 and 8083, 
as mentionned in the 'Main.java' file.

