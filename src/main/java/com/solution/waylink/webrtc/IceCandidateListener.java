package com.solution.waylink.webrtc;

import dev.onvoid.webrtc.RTCIceCandidate;

@FunctionalInterface
public interface IceCandidateListener {
    void onLocalCandidateGenerated(RTCIceCandidate candidate);
}
