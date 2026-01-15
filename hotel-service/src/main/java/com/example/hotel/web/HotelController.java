package com.example.hotel.web;

import com.example.hotel.dto.HotelRequest;
import com.example.hotel.dto.HotelResponse;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping
    public List<HotelResponse> list() {
        return hotelService.listHotels();
    }

    @GetMapping("/{id}")
    public HotelResponse get(@PathVariable Long id) {
        return hotelService.getHotel(id);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse create(@Valid @RequestBody HotelRequest request) {
        return hotelService.createHotel(request);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PutMapping("/{id}")
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return hotelService.updateHotel(id, request);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.deleteHotel(id);
    }

    @GetMapping("/rooms")
    public List<RoomResponse> rooms() {
        return hotelService.listRooms();
    }
}


