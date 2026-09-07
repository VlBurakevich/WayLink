package com.solution.waylink.host.control.native_impl;

public interface NativeInputController {
    void moveMouse(int x, int y);
    void mousePress(int buttonMask);
    void mouseRelease(int buttonMask);
    void mouseScroll(int deltaY, int deltaX);
    void keyPress(int keyCode);
    void keyRelease(int keyCode);
    void setClipboardText(String text);
    void releaseAll();
}
