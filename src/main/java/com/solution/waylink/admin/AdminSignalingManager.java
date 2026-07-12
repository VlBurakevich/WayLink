package com.solution.waylink.admin;

import com.solution.waylink.signaling.SignalingClient;
import com.solution.waylink.signaling.json.GsonMapper;
import com.solution.waylink.signaling.json.JsonMapper;
import com.solution.waylink.webrtc.PeerConnectionManager;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AdminSignalingManager {
    private final SignalingClient signalingClient;
    private final PeerConnectionManager peerConnectionManager;
    private final JsonMapper jsonMapper;

    public AdminSignalingManager(SignalingClient signalingClient, PeerConnectionManager peerConnectionManager) {
        this.signalingClient = signalingClient;
        this.peerConnectionManager = peerConnectionManager;
        this.jsonMapper = new GsonMapper();

        setupSignalingHandlers();
    }

    private void setupSignalingHandlers() {
        signalingClient.registerHandler("SDP_ANSWER", this::handleSdpAnswer);
        signalingClient.registerHandler("ICE_CANDIDATE", this::handleRemoteIceCandidate);
    }

    public void startWebRtcHandshake() {
        log.info("Initiating WebRTC Handshake from Admin side...");

        if (peerConnectionManager.getRtcPeerConnection() == null) {
            peerConnectionManager.createPeerConnection(this::sendIceCandidateToHost);
        }

        var pc = peerConnectionManager.getRtcPeerConnection();

        createOfferAsync(pc)
                .thenCompose(offer -> setSessionDescriptionAsync(pc, offer, true).thenApply(v -> offer))
                .thenAccept(offer -> {
                    signalingClient.send("SDP_OFFER", offer.sdp);
                    log.info("SDP Offer successfully generated and send to Host!");
                })
                .exceptionally(ex -> {
                    log.error("Failed to initiate WebRTC Offer from Admin side", ex);
                    return null;
                });
    }

    private void handleSdpAnswer(String payload) {
        log.info("Received SDP Answer from Host");
        var pc = peerConnectionManager.getRtcPeerConnection();

        if (pc == null) {
            log.error("Cannot set remote description: peer connection is null");
            return;
        }

        var answer = new RTCSessionDescription(RTCSdpType.ANSWER, payload);

        setSessionDescriptionAsync(pc, answer, false)
                .thenRun(() -> log.info("Remote description (Answer) applied successfully! Connection established"))
                .exceptionally(ex -> {
                    log.error("Failed to apply remote SDP Answer");
                    return null;
                });
    }

    private void handleRemoteIceCandidate(String payload) {
        try {
            var candidate = jsonMapper.fromJson(payload, RTCIceCandidate.class);
            if (peerConnectionManager.getRtcPeerConnection() != null) {
                peerConnectionManager.getRtcPeerConnection().addIceCandidate(candidate);
                log.debug("Added remote ICE candidate from Host");
            }
        } catch (Exception e) {
            log.error("Error adding remote ICE candidate", e);
        }
    }

    private void sendIceCandidateToHost(RTCIceCandidate candidate) {
        signalingClient.send("ICE_CANDIDATE", jsonMapper.toJson(candidate));
        log.debug("Send local ICE candidate to Host");
    }

    private CompletableFuture<RTCSessionDescription> createOfferAsync(RTCPeerConnection pc) {
        var future = new CompletableFuture<RTCSessionDescription>();
        var options = new RTCOfferOptions();

        pc.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override public void onSuccess(RTCSessionDescription desc) {
                future.complete(desc);
            }
            @Override public void onFailure(String err) {
                future.completeExceptionally(new RuntimeException(err));
            }
        });
        return future;
    }

    private CompletableFuture<Void> setSessionDescriptionAsync(RTCPeerConnection pc, RTCSessionDescription desc, boolean isLocal) {
        var future = new CompletableFuture<Void>();
        var observer = new SetSessionDescriptionObserver() {
            @Override public void onSuccess() { future.complete(null); }
            @Override public void onFailure(String err) { future.completeExceptionally(new RuntimeException(err)); }
        };

        if (isLocal) {
            pc.setLocalDescription(desc, observer);
        } else {
            pc.setRemoteDescription(desc, observer);
        }

        return future;
    }
}



