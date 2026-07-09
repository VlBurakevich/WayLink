package com.solution.waylink.host.control.native_impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class MacCoreGraphicsController implements NativeInputController {

    @SuppressWarnings("java:S100")
    private interface AppKit extends Library {
        AppKit INSTANCE = Native.load("AppKit", AppKit.class);

        Pointer NSPasteboardGeneralPasteboard();
        void NSPasteboardClearContents(Pointer pasteboard);
        void NSPasteboardSetStringForType(Pointer pasteboard, String string, String type);
    }

    @SuppressWarnings("java:S100")
    private interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        Pointer CGEventSourceCreate(int sourceState);
        Pointer CGEventCreateMouseEvent(Pointer source, int mouseType, double x, double y, int button);
        Pointer CGEventCreateKeyboardEvent(Pointer source, short virtualKey, boolean keyDown);
        void CGEventPost(int tap, Pointer event);

        Pointer CGEventCreateScrollWheelEvent(Pointer source, int units, int wheelCount, int wheel1, int wheel2);
    }

    private static final int CG_EVENT_MOUSE_MOVED = 5;
    private static final int CG_HID_EVENT_TAP = 0;

    private static final int CG_EVENT_LEFT_MOUSE_DOWN = 1;
    private static final int CG_EVENT_LEFT_MOUSE_UP = 2;
    private static final int CG_MOUSE_BUTTON_LEFT = 0;

    private static final int CG_EVENT_RIGHT_MOUSE_DOWN = 3;
    private static final int CG_EVENT_RIGHT_MOUSE_UP = 4;
    private static final int CG_MOUSE_BUTTON_RIGHT = 1;

    private static final int CG_SCROLL_EVENT_UNIT_LINE = 0;

    private static final String NSPASTEBOARD_TYPE_STRING = "public.utf8-plain-text";

    private final Pointer eventSource = CoreGraphics.INSTANCE.CGEventSourceCreate(0);

    @Override
    public void moveMouse(int x, int y) {
        Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(eventSource, CG_EVENT_MOUSE_MOVED, x, y, 0);
        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, event);
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
        int eventType;
        int button;

        if (buttonMask == 3) {
            eventType = isPress ? CG_EVENT_RIGHT_MOUSE_DOWN : CG_EVENT_RIGHT_MOUSE_UP;
            button = CG_MOUSE_BUTTON_RIGHT;
        } else {
            eventType = isPress ? CG_EVENT_LEFT_MOUSE_DOWN : CG_EVENT_LEFT_MOUSE_UP;
            button = CG_MOUSE_BUTTON_LEFT;
        }

        Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(eventSource, eventType, 0, 0, button);
        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, event);
    }

    @Override
    public void mouseScroll(int deltaY, int deltaX) {
        Pointer event = CoreGraphics.INSTANCE.CGEventCreateScrollWheelEvent(
            eventSource, CG_SCROLL_EVENT_UNIT_LINE, 2, deltaY, deltaX
        );

        if (event != null) {
            CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, event);
        }
    }

    @Override
    public void keyPress(int keyCode) {
        Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(eventSource, (short) keyCode, true);
        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, event);
    }

    @Override
    public void keyRelease(int keyCode) {
        Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(eventSource, (short) keyCode, false);
        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, event);
    }

    @Override
    public void setClipboardText(String text) {
        if (text == null) return;

        Pointer pasteboard = AppKit.INSTANCE.NSPasteboardGeneralPasteboard();
        if (pasteboard != null) {
            AppKit.INSTANCE.NSPasteboardClearContents(pasteboard);
            AppKit.INSTANCE.NSPasteboardSetStringForType(pasteboard, text, NSPASTEBOARD_TYPE_STRING);
        }
    }

    @Override
    public void releaseAll() {
        Pointer releaseLeft = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(eventSource, CG_EVENT_LEFT_MOUSE_UP, 0, 0, CG_MOUSE_BUTTON_LEFT);
        Pointer releaseRight = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(eventSource, CG_EVENT_RIGHT_MOUSE_UP, 0, 0, CG_MOUSE_BUTTON_RIGHT);

        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, releaseLeft);
        CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, releaseRight);

        short[] modifierKeyCodes = {
                0x38,
                0x3A,
                0x3B,
                0x37
        };

        for (short keyCode : modifierKeyCodes) {
            Pointer keyEvent = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(eventSource, keyCode, false);
            CoreGraphics.INSTANCE.CGEventPost(CG_HID_EVENT_TAP, keyEvent);
        }
    }
}
