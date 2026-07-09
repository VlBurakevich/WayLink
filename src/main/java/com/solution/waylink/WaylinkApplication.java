package com.solution.waylink;

import com.google.gson.Gson;
import com.solution.waylink.core.SignalingBridge;
import com.solution.waylink.signaling.SignalingClient;
import com.solution.waylink.signaling.dto.IceCandidateDto;
import com.solution.waylink.webrtc.PeerConnectionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WaylinkApplication extends Application {
    private static final Logger logger = Logger.getLogger(WaylinkApplication.class.getName());

    private PeerConnectionManager webrtcManager;
    private SignalingClient signalingClient;
    private SignalingBridge signalingBridge;
    private final Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.log(Level.INFO, "Запуск графического интерфейса");

        Label statusLabel = new Label("Инициализация компонентов...");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00F5FF;");

        StackPane root = new StackPane(statusLabel);
        root.setStyle("-fx-background-color: #0A0F1E;");

        Scene scene = new Scene(root, 400, 200);
        primaryStage.setTitle("WayLink Remote Desktop");
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread initThread = new Thread(() -> {
            try {
                initCoreComponents();

                Platform.runLater(() -> statusLabel.setText("Подключение к синальному серверу"));
                signalingClient.connect("localhost", 8080, "test-room", "1234");

                Platform.runLater(() -> statusLabel.setText("Готов к подключению(X11)"));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Ошибка инициализации ядра системы", e);
                Platform.runLater(() -> statusLabel.setText("Ошибка запуска: " + e.getMessage()));
            }
        });

        initThread.setDaemon(true);
        initThread.start();
    }

    private void initCoreComponents() {
        this.webrtcManager = new PeerConnectionManager();
        this.signalingClient = new SignalingClient();

        this.webrtcManager.createPeerConnection(candidate -> {
            IceCandidateDto dto = new IceCandidateDto(
                    candidate.sdpMid,
                    candidate.sdpMLineIndex,
                    candidate.sdp
            );

            signalingClient.send("candidate", gson.toJson(dto));
            logger.log(Level.INFO, "Локальный ICE кандидат отправлен в сеть");
        });

        this.signalingBridge = new SignalingBridge(signalingClient, webrtcManager);
        logger.log(Level.INFO, "Ядро webrtc и сигнальный мост успешно собраны");
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "Приложение закрывается. Освобождаем нативные ресурсы");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
