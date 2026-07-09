package com.solution.waylink.host.control.native_impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class X11InputController implements NativeInputController {

    @SuppressWarnings("java:S100")
    private interface X11 extends Library {
        X11 INSTANCE = Native.load("X11", X11.class);

        Pointer XOpenDisplay(String displayName);
        void XCloseDisplay(Pointer display);
        void XFlush(Pointer display);
    }


    @SuppressWarnings("java:S100")
    private interface Xtst extends X11 {
        Xtst INSTANCE = Native.load("Xtst", Xtst.class);

        void XTestFakeMotionEvent(Pointer display, int screen, int x, int y, long delay);
        void XTestFakeButtonEvent(Pointer display, int button, boolean isPress, long delay);
        void XTestFakeKeyEvent(Pointer display, int keyCode, boolean isPress, long delay);
    }

    private static final long X_CURRENT_DELAY = 0;
    private static final int X_CURRENT_SCREEN = -1;

    private static final int X_BUTTON_LEFT = 1;
    private static final int X_BUTTON_CENTER = 2;
    private static final int X_BUTTON_RIGHT  = 3;
    private static final int X_BUTTON_SCROLL_UP = 4;
    private static final int X_BUTTON_SCROLL_DOWN = 5;
    private static final int X_BUTTON_SCROLL_LEFT = 6;
    private static final int X_BUTTON_SCROLL_RIGHT = 7;

    private final Pointer display;

    public X11InputController() {
        this.display = X11.INSTANCE.XOpenDisplay(null);
        if (this.display == null) {
            throw new IllegalStateException("Cannot open Display, check env DISPLAY");
        }
    }

    @Override
    public void moveMouse(int x, int y) {
        Xtst.INSTANCE.XTestFakeMotionEvent(display, X_CURRENT_SCREEN, x, y, X_CURRENT_DELAY);
        X11.INSTANCE.XFlush(display);
    }

    @Override
    public void mousePress(int buttonMask) {
        sendMouseEvent(buttonMask, true);
    }

    @Override
    public void mouseRelease(int buttonMask) {
        sendMouseEvent(buttonMask, false);
    }

    private void sendMouseEvent(int buttonMask, boolean isPress) {
        int button = switch (buttonMask) {
            case 1 -> X_BUTTON_LEFT;
            case 2 -> X_BUTTON_CENTER;
            case 3 -> X_BUTTON_RIGHT;
            default -> -1;
        };

        if (button != -1) {
            Xtst.INSTANCE.XTestFakeButtonEvent(display, button, isPress, X_CURRENT_DELAY);
            X11.INSTANCE.XCloseDisplay(display);
        }
    }

    @Override
    public void mouseScroll(int deltaY, int deltaX) {
        if (deltaY != 0) {
            int button = deltaY > 0 ? X_BUTTON_SCROLL_UP : X_BUTTON_SCROLL_DOWN;
            sendScrollClicks(button, Math.abs(deltaX));
        }

        if (deltaX != 0) {
            int button = deltaX > 0 ? X_BUTTON_SCROLL_RIGHT : X_BUTTON_SCROLL_LEFT;
            sendScrollClicks(button, Math.abs(deltaX));
        }

        X11.INSTANCE.XFlush(display);
    }

    private void sendScrollClicks(int button, int clicks) {
        for (int i = 0; i < clicks; i++) {
            Xtst.INSTANCE.XTestFakeButtonEvent(display, button, true, X_CURRENT_DELAY);
            Xtst.INSTANCE.XTestFakeButtonEvent(display, button, false, X_CURRENT_DELAY);
        }
    }

    @Override
    public void keyPress(int keyCode) {
        Xtst.INSTANCE.XTestFakeKeyEvent(display, keyCode, true, X_CURRENT_DELAY);
        X11.INSTANCE.XFlush(display);
    }

    @Override
    public void keyRelease(int keyCode) {
        Xtst.INSTANCE.XTestFakeKeyEvent(display, keyCode, false, X_CURRENT_DELAY);
        X11.INSTANCE.XFlush(display);
    }

    @Override
    public void setClipboardText(String text) {
        if (text == null) return;

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);

    }

    @Override
    public void releaseAll() {
        int[] buttons = {X_BUTTON_LEFT, X_BUTTON_CENTER, X_BUTTON_RIGHT};
        for (int btn : buttons) {
            Xtst.INSTANCE.XTestFakeButtonEvent(display, btn, false, X_CURRENT_DELAY);
        }

        int[] modifierKeyCodes = {
                50,
                62,
                37,
                105,
                64,
                133
        };

        for (int keyCode : modifierKeyCodes) {
            Xtst.INSTANCE.XTestFakeKeyEvent(display, keyCode, false, X_CURRENT_DELAY);
        }

        X11.INSTANCE.XFlush(display);
    }

    public void close() {
        if (display != null) {
            X11.INSTANCE.XCloseDisplay(display);
        }
    }
}
