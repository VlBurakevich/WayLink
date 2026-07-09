package com.solution.waylink.webrtc;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;

import java.util.function.Consumer;

public final class WebRtcObservers {
    private WebRtcObservers() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SetSessionDescriptionObserver onSet(Runnable onSuccess, Consumer<String> onFailure) {
        return new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                onSuccess.run();
            }

            @Override
            public void onFailure(String err) {
                onFailure.accept(err);
            }
        };
    }

    public static CreateSessionDescriptionObserver onCreate(Consumer<RTCSessionDescription> onSuccess, Consumer<String> onFailure) {
        return new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                onSuccess.accept(description);
            }
            @Override
            public void onFailure(String err) {
                onFailure.accept(err);
            }
        };
    }
}
