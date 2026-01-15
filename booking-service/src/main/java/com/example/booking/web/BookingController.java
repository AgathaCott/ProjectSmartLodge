package com.example.booking.web;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookingCreateRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bookingService.createBooking(userId, request);
    }

    @GetMapping
    public List<BookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bookingService.getUserBookings(userId);
    }

    @GetMapping("/suggestions")
    public reactor.core.publisher.Mono<java.util.List<com.example.booking.service.BookingService.RoomView>> suggestions() {
        return bookingService.getRoomSuggestions();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public List<BookingResponse> all() {
        return bookingService.getAllBookings();
    }
}


