package com.zoodeck.identity;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zoodeck.identity.data.DataService;
import com.zoodeck.identity.data.DataServiceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zoodeck.identity.config.ConfigService;

import java.nio.charset.StandardCharsets;

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
        connectionFactory.setHost(configService.getHost());
        connectionFactory.setUsername(configService.getUsername());
        connectionFactory.setPassword(configService.getPassword());

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        // route registration
        channel.exchangeDeclare(ConstantsService.ROUTE_REGISTRATION_EXCHANGE, ConstantsService.FANOUT);

        // login
        JSONObject loginRoute = new JSONObject();
        loginRoute.put(ConstantsService.MESSAGE_TYPE, ConstantsService.LOGIN);
        loginRoute.put(ConstantsService.QUEUE_NAME, ConstantsService.LOGIN);
        channel.basicPublish(ConstantsService.ROUTE_REGISTRATION_EXCHANGE, ConstantsService.EMPTY_ROUTING_KEY, null, loginRoute.toString().getBytes(StandardCharsets.UTF_8));

        // messages-for-socket
        channel.exchangeDeclare(ConstantsService.MESSAGES_FOR_SOCKET_EXCHANGE, ConstantsService.FANOUT);

        // login
        channel.queueDeclare(ConstantsService.LOGIN, true, false, false, null);
        channel.basicConsume(ConstantsService.LOGIN, false, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("message: " + message);

            JSONObject jsonObject = new JSONObject(message);

            String socketId = jsonObject.getString(ConstantsService.SOCKET_ID);
            JSONObject socketIdProperties = jsonObject.getJSONObject(ConstantsService.SOCKET_ID_PROPERTIES);

            socketIdProperties.put("userId", "some-random-string");

            JSONObject socketMessage = new JSONObject();
            socketMessage.put(ConstantsService.SOCKET_ID, socketId);
            socketMessage.put(ConstantsService.PAYLOAD, "[1,2,3]");
            socketMessage.put(ConstantsService.SOCKET_ID_PROPERTIES, socketIdProperties);
            channel.basicPublish(ConstantsService.MESSAGES_FOR_SOCKET_EXCHANGE, ConstantsService.EMPTY_ROUTING_KEY, null, socketMessage.toString().getBytes(StandardCharsets.UTF_8));
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }, (consumerTag, sig) -> {

        });
    }
}
