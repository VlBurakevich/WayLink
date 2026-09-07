package com.solution.waylink.host.control.native_impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinUser.INPUT;

public class WindowsSendInputController implements NativeInputController{

    @SuppressWarnings("java:S100")
    private interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("Kernel32", Kernel32.class);
        Pointer GlobalAlloc(int uFlags, int dwBytes);
        Pointer GlobalLock(Pointer hMem);
        void GlobalUnlock(Pointer hMem);
        void RtlMoveMemory(Pointer dest, String src, int size);
    }


    @SuppressWarnings("java:S100")
    private interface User32Clipboard extends Library {
        User32Clipboard INSTANCE = Native.load("user32", User32Clipboard.class);
        boolean OpenClipboard(Pointer hWndNewOwner);
        void EmptyClipboard();
        void SetClipboardData(int uFormat, Pointer hMem);
        void CloseClipboard();
    }

    private static final int MOUSE_EVENTF_MOVE = 0x0001;
    private static final int MOUSE_EVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSE_EVENTF_LEFTUP = 0x0004;
    private static final int MOUSE_EVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSE_EVENTF_RIGHTUP = 0x0010;
    private static final int MOUSE_EVENTF_MIDDLEDOWN = 0x0020;
    private static final int MOUSE_EVENTF_MIDDLEUP = 0x0040;
    private static final int MOUSE_EVENTF_WHEEL = 0x0800;
    private static final int MOUSE_EVENTF_HWHEEL = 0x0100;
    private static final int MOUSE_EVENTF_ABSOLUTE = 0x8000;

    private static final int WHEEL_DELTA = 120;

    private static final int KEYEVENTF_KEYUP = 0x0002;

    private static final int GMEM_MOVEABLE = 0x0002;
    private static final int CF_UNICEDETEXT = 13;

    @Override
    public void moveMouse(int x, int y) {
        int screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN);
        int screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN);

        INPUT input = new INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");

        input.input.mi.dx = new LONG((x * 65536L) / screenWidth);
        input.input.mi.dy = new LONG((y * 65536L) / screenHeight);
        input.input.mi.dwFlags = new DWORD(MOUSE_EVENTF_MOVE | MOUSE_EVENTF_ABSOLUTE);
        input.input.mi.dwExtraInfo = new ULONG_PTR(0);

        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{input}, input.size());
    }

    @Override
    public void mousePress(int buttonMask) {
        sendMouseClick(buttonMask, true);
    }

    @Override
    public void mouseRelease(int buttonMask) {
        sendMouseClick(buttonMask, false);
    }

    private void sendMouseClick(int buttonMask, boolean press) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");

        int flags;
        switch (buttonMask) {
            case 1 -> flags = press ? MOUSE_EVENTF_LEFTDOWN : MOUSE_EVENTF_LEFTUP;
            case 2 -> flags = press ? MOUSE_EVENTF_MIDDLEDOWN : MOUSE_EVENTF_MIDDLEUP;
            case 3 -> flags = press ? MOUSE_EVENTF_RIGHTDOWN : MOUSE_EVENTF_RIGHTUP;
            default -> { return; }
        }

        input.input.mi.dwFlags = new DWORD(flags);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{input}, input.size());
    }

    @Override
    public void mouseScroll(int deltaY, int deltaX) {
        if (deltaY != 0) {
            INPUT inputY = createScrollInput(MOUSE_EVENTF_WHEEL, deltaY);
            User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{inputY}, inputY.size());
        }

        if (deltaX != 0) {
            INPUT inputX = createScrollInput(MOUSE_EVENTF_HWHEEL, deltaX);
            User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{inputX}, inputX.size());
        }
    }

    private INPUT createScrollInput(int flag, int delta) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        input.input.mi.dwFlags = new DWORD(flag);

        input.input.mi.mouseData = new DWORD((long) delta * WHEEL_DELTA);
        return input;
    }

    @Override
    public void keyPress(int keyCode) {
        sendKey(keyCode, false);
    }

    @Override
    public void keyRelease(int keyCode) {
        sendKey(keyCode, true);
    }

    private void sendKey(int keyCode, boolean isRelease) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(keyCode);
        input.input.ki.dwFlags = new DWORD(isRelease ? KEYEVENTF_KEYUP : 0);

        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{input}, input.size());
    }

    @Override
    public void setClipboardText(String text) {
        if (text == null) return;

        if (!User32Clipboard.INSTANCE.OpenClipboard(null)) return;

        try {
            User32Clipboard.INSTANCE.EmptyClipboard();

            int numBytes = (text.length() + 1) * 2;
            Pointer hMem = Kernel32.INSTANCE.GlobalAlloc(GMEM_MOVEABLE, numBytes);
            if (hMem == null) return;

            Pointer pMem = Kernel32.INSTANCE.GlobalLock(hMem);
            if (pMem != null) {
                Kernel32.INSTANCE.RtlMoveMemory(pMem, text, numBytes);
                Kernel32.INSTANCE.GlobalUnlock(hMem);

                User32Clipboard.INSTANCE.SetClipboardData(CF_UNICEDETEXT, hMem);
            }
        } finally {
            User32Clipboard.INSTANCE.CloseClipboard();
        }
    }

    @Override
    public void releaseAll() {
        int[] mouseUpFlags = { MOUSE_EVENTF_LEFTUP, MOUSE_EVENTF_MIDDLEDOWN, MOUSE_EVENTF_RIGHTUP};
        for (int flag : mouseUpFlags) {
            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType("mi");
            input.input.mi.dwFlags = new DWORD(flag);
            User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{input}, input.size());
        }

        int[] modifierVirtualKeys = {
                0x10,
                0x11,
                0x12,
                0x5B
        };

        for (int vk : modifierVirtualKeys) {
            sendKey(vk, true);
        }
    }
}
