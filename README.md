# MQTT load testing tool

This is a simple load testing tool that opens a specified number of connections to an MQTT 5.0 compliant server and
subscribes to a topic filter.

Connections are made synchronously (blocking mode).

Every 10 seconds, the total number of messages received is printed.

A topic subscription is made for each client, matching a specified filter.

3 parameters are required:

1. The URL of the MQTT server.
2. The number of connections to open.
3. The topic filter.


## Example usage:

```
java -jar mqtt-load-test-1.0-main.jar wss://hostname:port 10 'match/#'
```
