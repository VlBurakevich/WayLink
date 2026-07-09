package com.solution.waylink.webrtc;

import com.solution.waylink.host.capture.ScreenCapturerService;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import lombok.Getter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerConnectionManager {
    private static final Logger logger = Logger.getLogger(PeerConnectionManager.class.getName());

    private final PeerConnectionFactory peerConnectionFactory;

    @Getter
    private RTCPeerConnection rtcPeerConnection;

    @Getter
    private final ScreenCapturerService screenCapturerService;

    public PeerConnectionManager() {
        this.peerConnectionFactory = new PeerConnectionFactory();
        logger.log(Level.INFO, "WebRTC PCF initialized");

        this.screenCapturerService = new ScreenCapturerService(this.peerConnectionFactory);
        logger.log(Level.INFO, "ScreenCapturer service initialized ");
    }

    public void createPeerConnection(IceCandidateListener iceListener) {
        RTCConfiguration configuration = new RTCConfiguration();

        RTCIceServer stunServer = new RTCIceServer();
        stunServer.urls.add("stun:stun.l.google.com:19302");
        configuration.iceServers = List.of(stunServer);

        CustomPeerConnectionObserver observer = new CustomPeerConnectionObserver(iceListener);

        this.rtcPeerConnection = peerConnectionFactory.createPeerConnection(configuration, observer);
        logger.log(Level.INFO, "RTCPeerConnection created with candidate listener");
    }

    public void dispose() {
        if (screenCapturerService != null) {
            screenCapturerService.stopScreenSharing();
        }

        if (rtcPeerConnection != null) {
            rtcPeerConnection.close();
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }
        logger.log(Level.INFO, "WebRTC resources and capturer service released");
    }
}
