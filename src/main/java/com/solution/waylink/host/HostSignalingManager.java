package com.solution.waylink.host;

import com.solution.waylink.signaling.SignalingClient;
import com.solution.waylink.signaling.json.GsonMapper;
import com.solution.waylink.signaling.json.JsonMapper;
import com.solution.waylink.webrtc.PeerConnectionManager;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class HostSignalingManager {
    private final SignalingClient signalingClient;
    private final PeerConnectionManager peerConnectionManager;
    private final JsonMapper jsonMapper;

    public HostSignalingManager(SignalingClient signalingClient, PeerConnectionManager peerConnectionManager) {
        this.signalingClient = signalingClient;
        this.peerConnectionManager = peerConnectionManager;
        this.jsonMapper = new GsonMapper();

        setupSignalingHandlers();
    }

    private void setupSignalingHandlers() {
        signalingClient.registerHandler("SDP_OFFER", this::handleSdpOffer);
        signalingClient.registerHandler("ICE_CANDIDATE", this::handleRemoteIceCandidate);
    }

    private void handleSdpOffer(String payload) {
        log.info("Received SDP Offer from Admin");
        if (peerConnectionManager.getRtcPeerConnection() == null) {
            peerConnectionManager.createPeerConnection(this::sendIceCandidateToAdmin);
        }

        var pc = peerConnectionManager.getRtcPeerConnection();
        var offer = new RTCSessionDescription(RTCSdpType.OFFER, payload);

        setRemoteDescriptionAsync(pc, offer)
                .thenRun(() -> {
                    log.info("Remote SDP applied. Initializing screen sharing track...");
                    peerConnectionManager.getScreenCapturerService().startScreenSharing();
                    var videoTrack = peerConnectionManager.getScreenCapturerService().getVideoTrack();
                    pc.addTrack(videoTrack, List.of("WAYLINK_STREAM"));
                })
                .thenCompose(v -> createAnswerAsync(pc))
                .thenCompose(answer -> setLocalDescriptionAsync(pc, answer).thenApply(v -> answer))
                .thenAccept(answer -> {
                    signalingClient.send("SDP_ANSWER", answer.sdp);
                    log.info("SDP Answer successfully send to Admin!");
                })
                .exceptionally(ex -> {
                    log.error("Failed to establish WebRTC Handshake connection", ex);
                    return null;
                });
    }

    private void handleRemoteIceCandidate(String payload) {
        try {
            var candidate = jsonMapper.fromJson(payload, RTCIceCandidate.class);
            if (peerConnectionManager.getRtcPeerConnection() != null) {
                peerConnectionManager.getRtcPeerConnection().addIceCandidate(candidate);
                log.debug("Added remote ICE candidate");
            }
        } catch (Exception e) {
            log.error("Error adding remote ICE candidate", e);
        }
    }

    private void sendIceCandidateToAdmin(RTCIceCandidate candidate) {
        signalingClient.send("ICE_CANDIDATE", jsonMapper.toJson(candidate));
        log.debug("Send local ICE candidate to admin");
    }

    private CompletableFuture<Void> setRemoteDescriptionAsync(RTCPeerConnection pc, RTCSessionDescription desc) {
        var future = new CompletableFuture<Void>();
        pc.setRemoteDescription(desc, new SetSessionDescriptionObserver() {
            @Override public void onSuccess() { future.complete(null); }
            @Override public void onFailure(String err) { future.completeExceptionally(new RuntimeException(err)); }
        });
        return future;
    }

    private CompletableFuture<RTCSessionDescription> createAnswerAsync(RTCPeerConnection pc) {
        var future = new CompletableFuture<RTCSessionDescription>();
        var options = new RTCAnswerOptions();
        pc.createAnswer(options, new CreateSessionDescriptionObserver() {
            @Override public void onSuccess(RTCSessionDescription desc) {
                future.complete(desc);
            }
            @Override public void onFailure(String err) {
                future.completeExceptionally(new RuntimeException(err));
            }
        });
        return future;
    }

    private CompletableFuture<Void> setLocalDescriptionAsync(RTCPeerConnection pc, RTCSessionDescription desc) {
        var future = new CompletableFuture<Void>();
        pc.setLocalDescription(desc, new SetSessionDescriptionObserver() {
            @Override public void onSuccess() { future.complete(null); }
            @Override public void onFailure(String err) { future.completeExceptionally(new RuntimeException(err)); }
        });
        return future;
    }
}
