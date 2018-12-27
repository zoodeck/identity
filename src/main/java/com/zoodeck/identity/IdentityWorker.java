package com.zoodeck.identity;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zoodeck.common.config.ConfigService;
import com.zoodeck.identity.data.DataService;
import com.zoodeck.identity.data.DataServiceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static com.zoodeck.common.ConstantsService.*;
import static com.zoodeck.identity.InternalConstantsService.LOGIN;

public class IdentityWorker {
    private static Logger logger = LoggerFactory.getLogger(IdentityWorker.class);

    private ConfigService configService;
    private DataService dataService;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    public IdentityWorker(ConfigService configService) throws Exception {
        this.configService = configService;
        this.dataService = DataServiceFactory.getDataService(configService);
        setupRabbit();
    }

    private void setupRabbit() throws Exception {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(configService.get(RABBIT_HOST));
        connectionFactory.setUsername(configService.get(RABBIT_USER));
        connectionFactory.setPassword(configService.get(RABBIT_PASS));

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        // route registration
        channel.exchangeDeclare(ROUTE_REGISTRATION_EXCHANGE, FANOUT);

        // login
        JSONObject loginRoute = new JSONObject();
        loginRoute.put(MESSAGE_TYPE, LOGIN);
        loginRoute.put(QUEUE_NAME, LOGIN);
        channel.basicPublish(ROUTE_REGISTRATION_EXCHANGE, EMPTY_ROUTING_KEY, null, loginRoute.toString().getBytes(StandardCharsets.UTF_8));

        // messages-for-socket
        channel.exchangeDeclare(MESSAGES_FOR_SOCKET_EXCHANGE, FANOUT);

        // login
        channel.queueDeclare(LOGIN, true, false, false, null);
        channel.basicConsume(LOGIN, false, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("message: " + message);

            JSONObject jsonObject = new JSONObject(message);

            String socketId = jsonObject.getString(SOCKET_ID);
            JSONObject socketIdProperties = jsonObject.getJSONObject(SOCKET_ID_PROPERTIES);

            socketIdProperties.put("userId", "some-random-string");

            JSONObject socketMessage = new JSONObject();
            socketMessage.put(SOCKET_ID, socketId);
            socketMessage.put(PAYLOAD, "[1,2,3]");
            socketMessage.put(SOCKET_ID_PROPERTIES, socketIdProperties);
            channel.basicPublish(MESSAGES_FOR_SOCKET_EXCHANGE, EMPTY_ROUTING_KEY, null, socketMessage.toString().getBytes(StandardCharsets.UTF_8));
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }, (consumerTag, sig) -> {

        });
    }
}
