package com.solution.waylink.core;

import com.google.gson.Gson;
import com.solution.waylink.signaling.SignalingClient;
import com.solution.waylink.signaling.dto.IceCandidateDto;
import com.solution.waylink.signaling.dto.SessionDescriptionDto;
import com.solution.waylink.webrtc.PeerConnectionManager;
import com.solution.waylink.webrtc.WebRtcObservers;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SignalingBridge {
    private static final Logger logger = Logger.getLogger(SignalingBridge.class.getName());

    private static final String SIGNAL_OFFER = "offer";
    private static final String SIGNAL_ANSWER = "answer";
    private static final String SIGNAL_CANDIDATE = "candidate";

    private final SignalingClient signalingClient;
    private final PeerConnectionManager peerConnectionManager;
    private final Gson gson = new Gson();

    public SignalingBridge(SignalingClient signalingClient, PeerConnectionManager peerConnectionManager) {
        this.signalingClient = signalingClient;
        this.peerConnectionManager = peerConnectionManager;

        this.signalingClient.registerHandler(SIGNAL_OFFER, this::handleOffer);
        this.signalingClient.registerHandler(SIGNAL_ANSWER, this::handleAnswer);
        this.signalingClient.registerHandler(SIGNAL_CANDIDATE, this::handleIceCandidate);
    }

    private void handleOffer(String payload) {
        logger.log(Level.INFO, "handle offer");
        SessionDescriptionDto dto = gson.fromJson(payload, SessionDescriptionDto.class);

        RTCSessionDescription description = new RTCSessionDescription(
                RTCSdpType.OFFER,
                dto.getSdp()
        );

        var peerConnection = peerConnectionManager.getRtcPeerConnection();

        peerConnection.setRemoteDescription(description, WebRtcObservers.onSet(
                () -> {
                    logger.info("Remote OFFER successfully applied. Generating local ANSWER...");

                    peerConnection.createAnswer(new RTCAnswerOptions(), WebRtcObservers.onCreate(
                            this::handleLocalAnswerGenerated,
                            err -> logger.log(Level.SEVERE, "Create ANSWER failed: {0}", err)
                    ));
                },
                err -> logger.log(Level.SEVERE, "Set remote OFFER failed: {0}", err)
        ));
    }

    private void handleLocalAnswerGenerated(RTCSessionDescription localAnswer) {
        var peerConnection = peerConnectionManager.getRtcPeerConnection();

        peerConnection.setLocalDescription(localAnswer, WebRtcObservers.onSet(
                () -> {
                    logger.log(Level.INFO, "Local ANSWER successfully applied. Sending to remote peer");
                    SessionDescriptionDto answerDto = new SessionDescriptionDto(SIGNAL_ANSWER, localAnswer.sdp);
                    signalingClient.send(SIGNAL_ANSWER, gson.toJson(answerDto));
                },
                err -> logger.log(Level.SEVERE, "Failed to set local Answer: {0}", err)
        ));
    }

    private void handleAnswer(String payload) {
        logger.log(Level.INFO, "received remote Answer signaling channel");
        SessionDescriptionDto dto = gson.fromJson(payload, SessionDescriptionDto.class);

        RTCSessionDescription description = new RTCSessionDescription(
                RTCSdpType.ANSWER,
                dto.getSdp()
        );

        peerConnectionManager.getRtcPeerConnection().setRemoteDescription(description, WebRtcObservers.onSet(
                () -> logger.log(Level.INFO, "Remote ANSWER applied successfully. WebRTC Handshake completed"),
                err -> logger.log(Level.SEVERE, "Failed to apply remote Answer: {0}", err)
        ));
    }

    private void handleIceCandidate(String payload) {
        logger.log(Level.FINE, "Received Ice Candidate");
        IceCandidateDto dto = gson.fromJson(payload, IceCandidateDto.class);

        RTCIceCandidate candidate = new RTCIceCandidate(
                dto.getSdpMid(),
                dto.getSdpMLineIndex(),
                dto.getSdp()
        );

        peerConnectionManager.getRtcPeerConnection().addIceCandidate(candidate);
    }
}
