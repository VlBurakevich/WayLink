package com.solution.waylink.host.control.native_impl;

import java.io.IOException;

@SuppressWarnings("java:S1192")
public class WaylandInputController implements NativeInputController {

    public WaylandInputController() {
        try {
            Process process = new ProcessBuilder("ydotool", "--version").start();
            if (process.waitFor() != 0) {
                throw new IllegalArgumentException("ydotoold daemon is not running or accessible");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ydotool verification was interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("ydotool utility is missing in the system", e);
        }
    }


    @Override
    public void moveMouse(int x, int y) {
        executeCommand("ydotool", "mousemove", "-a", String.valueOf(x), String.valueOf(y));
    }

    @Override
    public void mousePress(int buttonMask) {
        sendMouseButton(buttonMask, true);
    }

    @Override
    public void mouseRelease(int buttonMask) {
        sendMouseButton(buttonMask, false);
    }
    private void sendMouseButton(int buttonMask, boolean isPress) {
        String button = switch (buttonMask) {
            case 1 -> "0x00";
            case 2 -> "0x02";
            case 3 -> "0x01";
            default -> null;
        };

        if (button != null) {
            String state = isPress ? "1" : "0";
            executeCommand("ydotool", "click", "--key", button, state);
        }
    }

    @Override
    public void mouseScroll(int deltaY, int deltaX) {
        if (deltaY != 0) {
            executeCommand("ydotool", "mousemove", "--wheel", String.valueOf(deltaY));
        }
        if (deltaX != 0) {
            executeCommand("ydotool", "mousemove", "--hwheel", String.valueOf(deltaX));
        }
    }

    @Override
    public void keyPress(int keyCode) {
        sendKeyEvent(keyCode, true);
    }

    @Override
    public void keyRelease(int keyCode) {
        sendKeyEvent(keyCode, false);
    }

    private void sendKeyEvent(int keyCode, boolean isPress) {
        String state = isPress ? "1": "0";
        executeCommand("ydotool", "key", keyCode + ":" + state);
    }

    @Override
    public void setClipboardText(String text) {
        if (text == null) return;

        try {
            Process process = new ProcessBuilder("wl-copy").start();
            try (var os = process.getOutputStream()) {
                os.write(text.getBytes());
                os.flush();
            }
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Suppress clipboard errors to prevent controller crashes
        }
    }

    @Override
    public void releaseAll() {
        int[] modifierLinuxKeycodes = {
                42,
                54,
                29,
                97,
                56,
                125
        };

        for (int code : modifierLinuxKeycodes) {
            executeCommand("ydotool", "key", code + ":0");
        }

        executeCommand("ydotool", "click", "--key", "0x00", "0");
        executeCommand("ydotool", "click", "--key", "0x01", "0");
        executeCommand("ydotool", "click", "--key", "0x02", "0");
    }

    private void executeCommand(String... args) {
        try {
            new ProcessBuilder(args).start();
        } catch (IOException ignored) {
            // Ignore single event failures to prevent stream interruption
        }
    }
}
