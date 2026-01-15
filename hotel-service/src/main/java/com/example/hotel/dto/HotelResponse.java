package com.example.hotel.dto;

import com.example.hotel.model.Hotel;

public class HotelResponse {
    private Long id;
    private String name;
    private String city;
    private String address;
    private int roomCount;

    public HotelResponse() {}

    public static HotelResponse fromEntity(Hotel hotel) {
        HotelResponse response = new HotelResponse();
        response.setId(hotel.getId());
        response.setName(hotel.getName());
        response.setCity(hotel.getCity());
        response.setAddress(hotel.getAddress());
        response.setRoomCount(hotel.getRooms() != null ? hotel.getRooms().size() : 0);
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getRoomCount() { return roomCount; }
    public void setRoomCount(int roomCount) { this.roomCount = roomCount; }
}
