package com.solution.waylink.client.input;

import java.nio.ByteBuffer;

public class AdminCommandEncoder {
    private static final byte CMD_MOUSE_MOVE = 1;
    private static final byte CMD_MOUSE_CLICK = 2;
    private static final byte CMD_KEYBOARD = 3;

    private AdminCommandEncoder() {

    }

    public static byte[] encodeMouseMove(int x, int y) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(CMD_MOUSE_MOVE);
        buffer.putInt(x);
        buffer.putInt(y);
        return buffer.array();
    }

    public static byte[] encodeMouseClick(boolean isPress, int buttonMask) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(CMD_MOUSE_CLICK);
        buffer.put((byte) (isPress ? 1 : 0));
        buffer.putInt(buttonMask);
        return buffer.array();
    }

    public static byte[] encodeKeyboard(boolean isPress, int keyCode) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(CMD_KEYBOARD);
        buffer.put((byte) (isPress ? 1 : 0));
        buffer.putInt(keyCode);
        return buffer.array();
    }
}
