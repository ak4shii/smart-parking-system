package com.smart_parking_system.backend.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT Configuration for Smart Parking System.
 * 
 * Topic Structure (secured with per-device ACL):
 * - sps/{mqttUsername}/entry/request - Entry gate requests
 * - sps/{mqttUsername}/exit/request - Exit gate requests
 * - sps/{mqttUsername}/status - Device status updates
 * - sps/{mqttUsername}/sensor/status - Sensor data
 * - sps/{mqttUsername}/provision/request - Device provisioning
 * - sps/{mqttUsername}/command - Commands to device
 * - sps/{mqttUsername}/camera - Camera triggers
 * 
 * Where mqttUsername = {ownerUsername}_{mcCode}
 * Example: sps/john_mc12345678/sensor/status
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-uri}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.base-topic}")
    private String baseTopic;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttEntryLogInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttMicrocontrollerInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttSensorInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttProvisionChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttEntryRequestChannel() {
        return new PublishSubscribeChannel();
    }

    /**
     * Subscribe to entry/exit gate requests.
     * Topic pattern: sps/+/entry/request, sps/+/exit/request
     * Where + matches {username}_{mcCode}
     */
    @Bean
    public MessageProducer inbound() {
        String[] topics = {
                baseTopic + "/+/entry/request",
                baseTopic + "/+/exit/request"
        };

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-inbound",
                mqttClientFactory(),
                topics);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttEntryRequestChannel());
        return adapter;
    }

    /**
     * Subscribe to device status updates.
     * Topic pattern: sps/+/status
     */
    @Bean
    public MessageProducer statusInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-status-inbound",
                mqttClientFactory(),
                baseTopic + "/+/status");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttMicrocontrollerInputChannel());
        return adapter;
    }

    /**
     * Subscribe to sensor status updates.
     * Topic pattern: sps/+/sensor/status
     */
    @Bean
    public MessageProducer sensorInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-sensor-inbound",
                mqttClientFactory(),
                baseTopic + "/+/sensor/status");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttSensorInputChannel());
        return adapter;
    }

    /**
     * Subscribe to device provisioning requests.
     * Topic pattern: sps/+/provision/request
     */
    @Bean
    public MessageProducer provisionInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-provision-inbound",
                mqttClientFactory(),
                baseTopic + "/+/provision/request");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttProvisionChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                clientId + "-outbound",
                mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(1);
        return messageHandler;
    }
}
