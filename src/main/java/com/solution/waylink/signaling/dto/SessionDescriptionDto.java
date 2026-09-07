package com.solution.waylink.signaling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDescriptionDto {
    private String type;
    private String sdp;
}
