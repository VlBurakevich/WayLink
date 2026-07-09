package com.solution.waylink.signaling;

import com.solution.waylink.signaling.dto.SignalingMessageDto;
import com.solution.waylink.signaling.handler.MessageHandler;
import com.solution.waylink.signaling.json.GsonMapper;
import com.solution.waylink.signaling.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignalingClient {
    private static final Logger logger = Logger.getLogger(SignalingClient.class.getName());

    private final JsonMapper jsonMapper;
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private WebSocket webSocket;

    public SignalingClient() {
        this.jsonMapper = new GsonMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    public void registerHandler(String messageType, MessageHandler handler) {
        this.handlers.put(messageType, handler);
    }

    public void connect(String host, int port, String room, String pin) {
        String url = String.format("ws://%s:%d/ws?room=%s&pin=%s",  host, port, room, pin);
        logger.log(Level.INFO, "Connect to signaling server: {0}", url);

        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    logger.log(Level.INFO, "Signaling WebSocket connection successfully established");
                })
                .exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Signaling connection handshake failed", ex);
                    return null;
                });
    }

    public void send(String type, String payload) {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            String json = jsonMapper.toJson(new SignalingMessageDto(type, payload));
            webSocket.sendText(json, true);
        } else {
            logger.log(Level.WARNING, "Cannot send, webSocket is no active");
        }
    }

    private class WebSocketListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                dispatch(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        private void dispatch(String rawJson) {
            try {
                SignalingMessageDto msg = jsonMapper.fromJson(rawJson, SignalingMessageDto.class);

                if ("error".equals(msg.getType())) {
                    logger.log(Level.WARNING, "Signaling server return error: {0}",  msg.getPayload());
                    return;
                }

                MessageHandler handler = handlers.get(msg.getType());
                if (handler != null) {
                    handler.handle(msg.getPayload());
                } else {
                    logger.log(Level.FINE, "No handler for message: {0}", msg.getType());
                }
            } catch (Exception err) {
                logger.log(Level.SEVERE, "Error while dispatching", err);
            }
        }

        @Override
        public void onError(WebSocket webSocket, Throwable err) {
            logger.log(Level.SEVERE, "WebSocket error", err);
        }
    }
}
