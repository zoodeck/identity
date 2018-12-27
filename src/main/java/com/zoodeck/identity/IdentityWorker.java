package com.zoodeck.identity;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zoodeck.common.config.ConfigService;
import com.zoodeck.identity.database.DatabaseService;
import com.zoodeck.identity.database.DatabaseServiceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static com.zoodeck.common.ConstantsService.*;
import static com.zoodeck.identity.InternalConstantsService.AUTHORIZE_USER;
import static com.zoodeck.identity.InternalConstantsService.REGISTER_USER;

public class IdentityWorker {
    private static Logger logger = LoggerFactory.getLogger(IdentityWorker.class);

    private ConfigService configService;
    private DatabaseService databaseService;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    public IdentityWorker(ConfigService configService) throws Exception {
        this.configService = configService;
        this.databaseService = DatabaseServiceFactory.getDatabaseService(configService);
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

        // messages-for-socket
        channel.exchangeDeclare(MESSAGES_FOR_SOCKET_EXCHANGE, FANOUT);


        // REGISTER_USER
        JSONObject registerUserRoute = new JSONObject();
        registerUserRoute.put(MESSAGE_TYPE, REGISTER_USER);
        registerUserRoute.put(QUEUE_NAME, REGISTER_USER);
        channel.basicPublish(ROUTE_REGISTRATION_EXCHANGE, EMPTY_ROUTING_KEY, null, registerUserRoute.toString().getBytes(StandardCharsets.UTF_8));

        // REGISTER_USER
        channel.queueDeclare(REGISTER_USER, true, false, false, null);
        channel.basicConsume(REGISTER_USER, false, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("message: " + message);

            JSONObject jsonObject = new JSONObject(message);
            String email = jsonObject.getString("email");
            String password = jsonObject.getString("password");

            JSONObject registerResult = databaseService.registerUser(email, password);

            String socketId = jsonObject.getString(SOCKET_ID);
            JSONObject socketIdProperties = jsonObject.getJSONObject(SOCKET_ID_PROPERTIES);

            JSONObject socketMessage = new JSONObject();
            socketMessage.put(SOCKET_ID, socketId);
            socketMessage.put(PAYLOAD, registerResult.toString());
            socketMessage.put(SOCKET_ID_PROPERTIES, socketIdProperties);
            channel.basicPublish(MESSAGES_FOR_SOCKET_EXCHANGE, EMPTY_ROUTING_KEY, null, socketMessage.toString().getBytes(StandardCharsets.UTF_8));
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }, (consumerTag, sig) -> {

        });

        // AUTHORIZE_USER
        JSONObject authorizeUserRoute = new JSONObject();
        authorizeUserRoute.put(MESSAGE_TYPE, AUTHORIZE_USER);
        authorizeUserRoute.put(QUEUE_NAME, AUTHORIZE_USER);
        channel.basicPublish(ROUTE_REGISTRATION_EXCHANGE, EMPTY_ROUTING_KEY, null, authorizeUserRoute.toString().getBytes(StandardCharsets.UTF_8));

        // AUTHORIZE_USER
        channel.queueDeclare(AUTHORIZE_USER, true, false, false, null);
        channel.basicConsume(AUTHORIZE_USER, false, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("message: " + message);

            JSONObject jsonObject = new JSONObject(message);
            String email = jsonObject.getString("email");
            String password = jsonObject.getString("password");

            JSONObject authorizeResult = databaseService.authorizeUser(email, password, null);

            String socketId = jsonObject.getString(SOCKET_ID);
            JSONObject socketIdProperties = jsonObject.getJSONObject(SOCKET_ID_PROPERTIES);

            JSONObject socketMessage = new JSONObject();
            socketMessage.put(SOCKET_ID, socketId);
            socketMessage.put(PAYLOAD, authorizeResult.toString());
            socketMessage.put(SOCKET_ID_PROPERTIES, socketIdProperties);
            channel.basicPublish(MESSAGES_FOR_SOCKET_EXCHANGE, EMPTY_ROUTING_KEY, null, socketMessage.toString().getBytes(StandardCharsets.UTF_8));
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }, (consumerTag, sig) -> {

        });
    }
}
