package com.example.hotel.dto;

import com.example.hotel.model.RoomReservationLock;
import java.time.LocalDate;

public class RoomLockResponse {
    private Long id;
    private String requestId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public RoomLockResponse() {}

    public static RoomLockResponse fromEntity(RoomReservationLock lock) {
        RoomLockResponse response = new RoomLockResponse();
        response.setId(lock.getId());
        response.setRequestId(lock.getRequestId());
        response.setRoomId(lock.getRoomId());
        response.setStartDate(lock.getStartDate());
        response.setEndDate(lock.getEndDate());
        response.setStatus(lock.getStatus().name());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
