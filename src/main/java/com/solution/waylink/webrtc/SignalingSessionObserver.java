package com.solution.waylink.webrtc;

import dev.onvoid.webrtc.SetSessionDescriptionObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public record SignalingSessionObserver(
        String operationName,
        Runnable onSuccessHook
) implements SetSessionDescriptionObserver {
    private static final Logger logger = Logger.getLogger(SignalingSessionObserver.class.getName());

    public SignalingSessionObserver(String operationName) {
        this(operationName, null);
    }

    @Override
    public void onSuccess() {
        logger.log(Level.INFO, "{0} set as RemoteDescription", operationName);
    }

    @Override
    public void onFailure(String err) {
        logger.log(Level.INFO, "failed to set RemoteDescription for {0}, error {1}", new Object[]{operationName, err} );
    }
}
