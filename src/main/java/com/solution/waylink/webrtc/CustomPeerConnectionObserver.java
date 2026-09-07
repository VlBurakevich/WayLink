package com.solution.waylink.webrtc;

import com.solution.waylink.host.control.InputControlService;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCDataChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomPeerConnectionObserver implements PeerConnectionObserver {
    private static final Logger logger = Logger.getLogger(CustomPeerConnectionObserver.class.getName());

    private final InputControlService inputControlService = new InputControlService();
    private final IceCandidateListener iceCandidateListener;

    public CustomPeerConnectionObserver(IceCandidateListener iceCandidateListener) {
        this.iceCandidateListener = iceCandidateListener;
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        logger.log(Level.INFO, "Generated local ICE Candidate: {0}", candidate);
        if (iceCandidateListener != null) {
            iceCandidateListener.onLocalCandidateGenerated(candidate);
        }
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        logger.log(Level.INFO, "Remote peer opened a DataChannel: {0}", dataChannel.getLabel());

        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {}

            @Override
            public void onStateChange() {
                logger.log(Level.INFO, "DataChannel [{0}] state changed to: {1}", new Object[]{dataChannel.getLabel(), dataChannel.getState()});
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                inputControlService.handleIncomingMessage(buffer);
            }
        });
    }

    @Override
    public void onConnectionChange(RTCPeerConnectionState state) {
        logger.log(Level.INFO, "WebRTC Connection state changed to {0}", state);
        if (state == RTCPeerConnectionState.CONNECTED) {
            logger.log(Level.INFO, "P2P Tunnel successfully established! Network traffic flows directly.");
        }
    }
}
