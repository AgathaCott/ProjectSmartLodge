package com.example.hotel.web;

import com.example.hotel.dto.*;
import com.example.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class RoomController {
    private final HotelService hotelService;

    public RoomController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return hotelService.getRoom(id);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return hotelService.createRoom(request);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return hotelService.updateRoom(id, request);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.deleteRoom(id);
    }

    // Hold availability
    @PostMapping("/{id}/hold")
    public RoomLockResponse hold(@PathVariable Long id, @Valid @RequestBody RoomHoldRequest request) {
        return hotelService.holdRoom(request, id);
    }

    @PostMapping("/{id}/confirm")
    public RoomLockResponse confirm(@PathVariable Long id, @Valid @RequestBody RoomActionRequest request) {
        return hotelService.confirmHold(request);
    }

    @PostMapping("/{id}/release")
    public RoomLockResponse release(@PathVariable Long id, @Valid @RequestBody RoomActionRequest request) {
        return hotelService.releaseHold(request);
    }
}


