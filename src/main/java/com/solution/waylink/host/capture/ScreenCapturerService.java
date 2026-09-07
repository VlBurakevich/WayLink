package com.solution.waylink.host.capture;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.video.VideoTrack;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScreenCapturerService {
    private final PeerConnectionFactory factory;
    private WebRTCScreenCapturer screenCapturer;

    public ScreenCapturerService(PeerConnectionFactory factory) {
        this.factory = factory;
    }

    public synchronized void startScreenSharing() {
        if (screenCapturer != null) {
            log.warn("Screen sharing is already active.");
            return;
        }

        log.info("Starting screen sharing.");
        try {
            screenCapturer = new WebRTCScreenCapturer(factory);
            screenCapturer.startCapture();
            log.info("Screen sharing is started.");
        } catch (Exception e) {
            log.error("Failed to start screen sharing.", e);
            stopScreenSharing();
            throw e;
        }
    }

    public synchronized VideoTrack getVideoTrack() {
        if (screenCapturer == null) {
            throw new IllegalStateException("Screen sharing is not active.");
        }
        return screenCapturer.getVideoTrack();
    }

    public synchronized boolean isSharing() {
        return screenCapturer != null;
    }

    public synchronized void stopScreenSharing() {
        if (screenCapturer == null) {
            return;
        }

        log.info("Stopping screen sharing.");
        try {
            screenCapturer.dispose();
        } catch (Exception e) {
            log.error("Failed while screen sharing.", e);
        } finally {
            screenCapturer = null;
            log.info("Screen sharing is stopped.");
        }
    }
}
