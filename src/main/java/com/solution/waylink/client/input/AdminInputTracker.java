package com.solution.waylink.client.input;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javafx.scene.input.MouseEvent;

@Slf4j
public class AdminInputTracker {
    public interface DataSender {
        void send(byte[] bytes);
    }

    private final DataSender dataSender;

    @Setter
    private int remoteWidth = 1920;
    @Setter
    private int remoteHeight = 1080;

    public AdminInputTracker(DataSender dataSender) {
        this.dataSender = dataSender;
    }

    public void attach(ImageView targetView) {
        targetView.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            double localWidth = targetView.getBoundsInLocal().getWidth();
            double localHeight = targetView.getBoundsInLocal().getHeight();

            if (localWidth <= 0 || localHeight <= 0) return;

            int remoteX = (int) ((event.getX() / localWidth) * remoteWidth);
            int remoteY = (int) ((event.getX() / localHeight) * remoteHeight);

            byte [] packet = AdminCommandEncoder.encodeMouseMove(remoteX, remoteY);
            dataSender.send(packet);
        });

        targetView.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            int mask = getButtonMask(event);
            if (mask != 0) {
                byte[] packet = AdminCommandEncoder.encodeMouseClick(true, mask);
                dataSender.send(packet);
            }
            targetView.requestFocus();
        });

        targetView.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
           int mask = getButtonMask(event);
           if (mask != 0) {
               byte[] packet = AdminCommandEncoder.encodeMouseClick(false, mask);
               dataSender.send(packet);
           }
        });

        targetView.setFocusTraversable(true);

        targetView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            int keyCode = event.getCode().getCode();
            byte[] packet = AdminCommandEncoder.encodeKeyboard(true, keyCode);
            dataSender.send(packet);
        });

        targetView.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            int keyCode = event.getCode().getCode();
            byte[] packet = AdminCommandEncoder.encodeKeyboard(false, keyCode);
            dataSender.send(packet);
        });
    }

    private int getButtonMask(MouseEvent event) {
        return switch (event.getButton()) {
            case PRIMARY -> 1;
            case MIDDLE -> 2;
            case SECONDARY -> 3;
            default -> 0;
        };
    }
}
