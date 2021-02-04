package com.pushtechnology.utils;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Main {

    private class MyConsumer implements Consumer<Mqtt5Publish> {
        private final AtomicInteger count = new AtomicInteger(0);

        public MyConsumer() {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                        System.out.println("Updates received: " + count.get());
                    }, 0, 10L, TimeUnit.SECONDS);
        }

        @Override
        public void accept(Mqtt5Publish mqtt5Publish) {
            count.incrementAndGet();
        }
    }

    private class MyConnectedListener implements MqttClientConnectedListener {
        private final String id;
        public MyConnectedListener(String id) {
            this.id = id;
        }
        @Override
        public void onConnected(MqttClientConnectedContext context) {
            System.out.println("CONNECT: " + id + " : " + context.getClientConfig().getState());
        }
    }

    public class MyDisconnectedListener implements MqttClientDisconnectedListener {
        private final String id;
        public MyDisconnectedListener(String id) {
            this.id = id;
        }
        @Override
        public void onDisconnected(MqttClientDisconnectedContext context) {
            System.out.println("DISCONNECT: " + id + " " + context.getCause().getMessage());
        }
    }

    private final boolean useSSL;
    public final String host;
    public final int port;
    public final int sessionCount;
    public final String topicFilter;

    public MyConsumer onMessageCallback = new MyConsumer();

    public Main(String url, int sessionCount, String topicFilter) {
        URI uri = URI.create(url);
        this.useSSL = uri.getScheme().equals("wss") || uri.getScheme().equals("mqtts") ? true : false;
        this.host = uri.getHost();
        this.port = uri.getPort();

        this.sessionCount = sessionCount;
        this.topicFilter = topicFilter;

        System.out.println("Using SSL? " + useSSL);
    }

    public void run() {
        for (int i = 0; i < sessionCount; i++) {
            String clientId = "client-" + i;

            Mqtt5ClientBuilder builder = MqttClient.builder()
                    .serverHost(host)
                    .serverPort(port)
                    .useMqttVersion5()
                    .addConnectedListener(new MyConnectedListener(clientId))
                    .addDisconnectedListener(new MyDisconnectedListener(clientId))
                    .automaticReconnectWithDefaultConfig();

            if(useSSL) {
                builder = builder.sslWithDefaultConfig();
            }

            Mqtt5Client client = builder.build();

            client.toBlocking().connect();
            client.toAsync().subscribeWith()
                    .topicFilter(topicFilter)
                    .callback(onMessageCallback)
                    .send();
        }
    }

    public static void main(String[] args) throws Exception {
        Main app = new Main(args[0], Integer.parseInt(args[1]), args[2]);
        app.run();
    }
}
