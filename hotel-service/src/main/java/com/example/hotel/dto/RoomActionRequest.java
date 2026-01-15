package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public class RoomActionRequest {
    @NotBlank(message = "Request ID is required")
    private String requestId;

    public RoomActionRequest() {}

    public RoomActionRequest(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
