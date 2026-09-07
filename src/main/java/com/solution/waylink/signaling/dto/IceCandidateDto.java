package com.solution.waylink.signaling.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IceCandidateDto {
    private String sdpMid;
    private int sdpMLineIndex;
    private String sdp;
}
