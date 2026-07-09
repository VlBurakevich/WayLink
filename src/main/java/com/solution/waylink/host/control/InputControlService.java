package com.solution.waylink.host.control;

import com.solution.waylink.host.control.native_impl.NativeInputController;
import dev.onvoid.webrtc.RTCDataChannelBuffer;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InputControlService {
    private static final Logger logger = Logger.getLogger(InputControlService.class.getName());

    private static final byte CMD_MOUSE_MOVE = 1;
    private static final byte CMD_MOUSE_CLICK = 2;
    private static final byte CMD_KEYBOARD = 3;

    private final NativeInputController inputController;

    public InputControlService() {
        this.inputController = InputControllerFactory.createController();
    }

    public void handleIncomingMessage(RTCDataChannelBuffer buffer) {
        if (!buffer.binary) return;

        ByteBuffer data = buffer.data;
        if (data.remaining() < 1) return;

        byte commandID = data.get();

        switch (commandID) {
            case CMD_MOUSE_MOVE:
                if (data.remaining() < 8) break;
                inputController.moveMouse(data.getInt(), data.getInt());
                break;

            case CMD_MOUSE_CLICK:
                if (data.remaining() < 5) break;
                handleMouseClick(data.get(), data.getInt());
                break;

            case CMD_KEYBOARD:
                if (data.remaining() < 5) break;
                handleKeyboard(data.get(), data.getInt());
                break;
            default:
                logger.log(Level.WARNING, "Unknown command: {0}", commandID);
        }
    }

    private void handleMouseClick(byte action, int mask) {
        if (action == 1) {
            inputController.mousePress(mask);
        } else {
            inputController.mouseRelease(mask);
        }
    }

    private void handleKeyboard(byte action, int keyCode) {
        if (action == 1) {
            inputController.keyPress(keyCode);
        } else {
            inputController.keyRelease(keyCode);
        }
    }
}
