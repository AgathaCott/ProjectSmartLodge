package com.example.hotel.dto;

import com.example.hotel.model.Room;

public class RoomResponse {
    private Long id;
    private String number;
    private int capacity;
    private long timesBooked;
    private boolean available;
    private Long hotelId;
    private String hotelName;

    public RoomResponse() {}

    public static RoomResponse fromEntity(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setNumber(room.getNumber());
        response.setCapacity(room.getCapacity());
        response.setTimesBooked(room.getTimesBooked());
        response.setAvailable(room.isAvailable());
        if (room.getHotel() != null) {
            response.setHotelId(room.getHotel().getId());
            response.setHotelName(room.getHotel().getName());
        }
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public long getTimesBooked() { return timesBooked; }
    public void setTimesBooked(long timesBooked) { this.timesBooked = timesBooked; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
}
