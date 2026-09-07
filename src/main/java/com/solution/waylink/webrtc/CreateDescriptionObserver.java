package com.solution.waylink.webrtc;

import com.google.gson.Gson;
import com.solution.waylink.signaling.SignalingClient;
import com.solution.waylink.signaling.dto.SessionDescriptionDto;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCSessionDescription;

import java.util.logging.Level;
import java.util.logging.Logger;

public record CreateDescriptionObserver(
        RTCPeerConnection peerConnection,
        SignalingClient signalingClient,
        String sdpType
) implements CreateSessionDescriptionObserver {
    private static final Logger logger = Logger.getLogger(CreateDescriptionObserver.class.getName());
    private static final Gson gson = new Gson();

    @Override
    public void onSuccess(RTCSessionDescription description) {
        logger.log(Level.INFO, "Local {0} generated successfully", sdpType);

        peerConnection.setLocalDescription(
                description,
                new SignalingSessionObserver("Local " + sdpType)
        );

        SessionDescriptionDto dto = new SessionDescriptionDto(sdpType, description.sdp);
        signalingClient.send(sdpType, gson.toJson(dto));
    }

    @Override
    public void onFailure(String err) {
        logger.log(Level.SEVERE, "Failed to create local {0}. Error: {1}", new Object[]{sdpType, err});
    }
}
