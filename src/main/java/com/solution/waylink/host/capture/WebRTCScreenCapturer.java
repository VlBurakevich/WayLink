package com.solution.waylink.host.capture;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.video.VideoDesktopSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebRTCScreenCapturer {

    private final PeerConnectionFactory factory;
    private VideoDesktopSource videoDesktopSource;
    private VideoTrack videoTrack;
    private boolean isCapturing = false;

    public WebRTCScreenCapturer(PeerConnectionFactory factory) {
        this.factory = factory;
    }

    public void startCapture() {
        if (isCapturing) {
            return;
        }

        try {
            this.videoDesktopSource = new VideoDesktopSource();
            this.videoTrack = factory.createVideoTrack("WAYLINK_SCREEN_STREAM", videoDesktopSource);
            this.isCapturing = true;
            log.info("WebRTC screen capture started successfully.");
        } catch (Exception e) {
            log.error("WebRTC screen capture: {}", e.getMessage(), e);
            throw e;
        }
    }

    public VideoTrack getVideoTrack() {
        if (!isCapturing || videoTrack == null) {
            throw new IllegalStateException("Capturer is not running");
        }
        return this.videoTrack;
    }

    public void dispose() {
        if (!isCapturing) {
            return;
        }

        if (videoTrack != null) {
            videoTrack.dispose();
            videoTrack = null;
        }

        if (videoDesktopSource != null) {
            videoDesktopSource.dispose();
            videoDesktopSource = null;
        }

        isCapturing = false;
        log.info("WebRTC screen capture stopped successfully.");
    }
}
