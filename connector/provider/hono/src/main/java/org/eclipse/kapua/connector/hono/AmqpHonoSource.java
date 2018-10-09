/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.connector.hono;

import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.hono.util.MessageHelper;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.broker.client.hono.HonoClient;
import org.eclipse.kapua.connector.AbstractAmqpSource;
import org.eclipse.kapua.connector.KapuaConverterException;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.connector.Properties;
import org.eclipse.kapua.message.transport.TransportMessageType;
import org.eclipse.kapua.message.transport.TransportQos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Amqp Hono connector implementation
 *
 */
public class AmqpHonoSource extends AbstractAmqpSource<byte[]> {

    protected final static Logger logger = LoggerFactory.getLogger(AmqpHonoSource.class);

    private final static String EVENT_PREFIX = "event/";
    private final static String TELEMETRY_PREFIX = "telemetry/";
    private final static String EVENT_PREFIX_SHORT = "e/";
    private final static String TELEMETRY_PREFIX_SHORT = "t/";
    private final static String ORIGINAL_ADDRESS = "orig_address";
    private final static String ORIGINAL_ADAPTER = "orig_adapter";
    private final static String DEVICE_ID = "device_id";
    private final static String TENANT_ID = "tenant_id";

    private HonoClient honoClient;

    public static AmqpHonoSource create(Vertx vertx, HonoClient consumer) {
        return new AmqpHonoSource(vertx, consumer);
    }

    protected AmqpHonoSource(Vertx vertx, HonoClient honoClient) {
        super(vertx);
        this.honoClient = honoClient;
        honoClient.messageHandler(this::handleTransportMessage);
    }

    @Override
    public void start(Future<Void> startFuture) {
        connect(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        disconnect(stopFuture);
    }

    @Override
    protected void connect(Future<Void> connectFuture) {
        logger.info("Opening broker connection...");
        honoClient.connect(connectFuture);
    }

    @Override
    protected void disconnect(Future<Void> disconnectFuture) {
        logger.info("Closing broker connection...");
        honoClient.disconnect(disconnectFuture);
    }

    private void handleTransportMessage(final Message message) {
        logTelemetryMessage(message);
        try {
            //TODO fix me!
            try {
                MessageContext<byte[]> msg = convert(message);
                if (isProcessMessage(msg)) {
                    messageHandler.handle(msg, result -> {
                    if (result.succeeded()) {
                        //TODO handle ProtonHelper.accepted
                    }
                    else {
                        //TODO handle ProtonHelper.released
                    }
                });
                }
            } catch (Exception e) {
                logger.error("Exception while processing message: {}", e.getMessage(), e);
                //TODO handle ProtonHelper.released
            }
        } catch (Exception e) {
            logger.error("Exception while processing message: {}", e.getMessage(), e);
        }
    }

    private void logTelemetryMessage(final Message msg) {
        String messageId, userId, to, adapter, origAddress;
        messageId = userId = to = adapter = origAddress = null;
        if (msg.getProperties()!=null) {
            messageId = msg.getProperties().getMessageId() != null ? msg.getProperties().getMessageId().toString() : null;
            userId = msg.getProperties().getUserId() != null ? msg.getProperties().getUserId().toString() : null;
            to = msg.getProperties().getTo();
        }
        if (msg.getApplicationProperties()!=null && msg.getApplicationProperties().getValue()!=null) {
            adapter = (String)msg.getApplicationProperties().getValue().get(ORIGINAL_ADAPTER);
            origAddress = (String)msg.getApplicationProperties().getValue().get(ORIGINAL_ADDRESS);
        }
        String deviceId = MessageHelper.getDeviceId(msg);
        String tenantId = MessageHelper.getTenantId(msg);
        if (tenantId==null && msg.getMessageAnnotations() != null && msg.getMessageAnnotations().getValue() != null) {
            tenantId = msg.getMessageAnnotations().getValue().get(Symbol.getSymbol(TENANT_ID)).toString();
        }
        if (logger.isInfoEnabled()) {
            String content = null;
            if (msg.getBody()!=null && ((Data) msg.getBody()).getValue()!=null) {
                content = ((Data) msg.getBody()).getValue().toString();
            }
            logger.info("received telemetry message:\n\tmessage id '{}' userId '{}' destination '{}' original destination '{}' adapter '{}' tenant '{}' - device '{}' - content-type '{}' - content {}", 
                    messageId, userId, to, origAddress, adapter, tenantId, deviceId, msg.getContentType(), content);
        }
    }


    protected MessageContext<byte[]> convert(Message message) throws KapuaException {
        //this cast is safe since this implementation is using the AMQP connector
        return new MessageContext<byte[]>(
                extractBytePayload(message.getBody()),
                getMessageParameters(message));

        // By default, the receiver automatically accepts (and settles) the delivery
        // when the handler returns, if no other disposition has been applied.
        // To change this and always manage dispositions yourself, use the
        // setAutoAccept method on the receiver.
    }

    @Override
    protected Map<String, Object> getMessageParameters(Message message) throws KapuaConverterException {
        Map<String, Object> parameters = new HashMap<>();
        // build the message properties
        // extract original MQTT topic
        String mqttTopic = null;
        String adapter = null;
        String deviceId = MessageHelper.getDeviceId(message);
        String tenantId = MessageHelper.getTenantId(message);
        String destination = message.getProperties().getTo();
        TransportMessageType transportMessageType = null;
        if (message.getApplicationProperties()!=null && message.getApplicationProperties().getValue()!=null) {
            adapter = (String)message.getApplicationProperties().getValue().get(ORIGINAL_ADAPTER);
            mqttTopic = (String)message.getApplicationProperties().getValue().get(ORIGINAL_ADDRESS);
            mqttTopic = mqttTopic.replace(".", "/");
            if (destination.startsWith(TELEMETRY_PREFIX)) {
                transportMessageType = TransportMessageType.TELEMETRY;
                if (mqttTopic.startsWith(TELEMETRY_PREFIX)) {
                    mqttTopic = mqttTopic.substring(TELEMETRY_PREFIX.length());
                }
                else if (mqttTopic.startsWith(TELEMETRY_PREFIX_SHORT)) {
                    mqttTopic = mqttTopic.substring(TELEMETRY_PREFIX_SHORT.length());
                }
                else {
                    logger.warn("Unrecognized mqtt topic '{}'. Valid values should start with '{}' or '{}'", mqttTopic, TELEMETRY_PREFIX, TELEMETRY_PREFIX_SHORT);
                }
            }
            else if (destination.startsWith(EVENT_PREFIX)) {
                transportMessageType = TransportMessageType.EVENTS;
                if (mqttTopic.startsWith(EVENT_PREFIX)) {
                    mqttTopic = mqttTopic.substring(EVENT_PREFIX.length());
                }
                else if (mqttTopic.startsWith(EVENT_PREFIX_SHORT)) {
                    mqttTopic = mqttTopic.substring(EVENT_PREFIX_SHORT.length());
                }
                else {
                    logger.warn("Unrecognized mqtt topic '{}'. Valid values should start with '{}' or '{}'", mqttTopic, EVENT_PREFIX, EVENT_PREFIX_SHORT);
                }
            }
            else {
                logger.warn("Unrecognized destination '{}'. Valid values should start with '{}' or '{}'", mqttTopic, TELEMETRY_PREFIX, EVENT_PREFIX);
            }
            if (deviceId==null) {
                deviceId = (String)message.getApplicationProperties().getValue().get(DEVICE_ID);
            }
        }
        if (tenantId==null && message.getMessageAnnotations() != null && message.getMessageAnnotations().getValue() != null) {
            tenantId = message.getMessageAnnotations().getValue().get(Symbol.getSymbol(TENANT_ID)).toString();
        }
        //TODO handle alerts, ... messages types
        parameters.put(Properties.MESSAGE_ORIGINAL_DESTINATION, mqttTopic);
        parameters.put(Properties.MESSAGE_TYPE, transportMessageType);

        parameters.put(Properties.MESSAGE_CLIENT_ID, deviceId);
        parameters.put(Properties.MESSAGE_DEVICE_ADAPTER, adapter);
        parameters.put(Properties.MESSAGE_SCOPE_NAME, tenantId);

        // extract the original QoS
        //TODO
        parameters.put(Properties.MESSAGE_QOS, TransportQos.AT_MOST_ONCE);
        return parameters;
    }

}
