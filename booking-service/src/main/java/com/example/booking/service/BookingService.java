package com.example.booking.service;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.exception.BookingException;
import com.example.booking.model.Booking;
import com.example.booking.repo.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    
    private final BookingRepository bookingRepository;
    private final WebClient webClient;
    private final int retries;
    private final Duration timeout;

    public BookingService(
            BookingRepository bookingRepository,
            WebClient.Builder builder,
            @Value("${hotel.base-url}") String hotelBaseUrl,
            @Value("${hotel.timeout-ms}") int timeoutMs,
            @Value("${hotel.retries}") int retries
    ) {
        this.bookingRepository = bookingRepository;
        this.webClient = builder.baseUrl(hotelBaseUrl).build();
        this.retries = retries;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingCreateRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        // Идемпотентность: если запрос с таким requestId уже обработан — возвращаем существующую запись
        Booking existing = bookingRepository.findByRequestId(request.getRequestId()).orElse(null);
        if (existing != null) {
            log.info("[{}] Booking with requestId {} already exists, returning existing", 
                    correlationId, request.getRequestId());
            return BookingResponse.fromEntity(existing);
        }
        
        validateBookingDates(request);
        
        Booking booking = createPendingBooking(userId, request, correlationId);
        booking = bookingRepository.save(booking);
        
        log.info("[{}] Booking PENDING created", correlationId);

        try {
            processBookingFlow(booking, correlationId);
            booking.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(booking);
            log.info("[{}] Booking CONFIRMED", correlationId);
        } catch (Exception e) {
            log.warn("[{}] Booking flow failed: {}", correlationId, e.toString());
            compensateBooking(booking, correlationId);
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
            log.info("[{}] Booking CANCELLED and compensated", correlationId);
            throw new BookingException("Booking failed: room is not available or service error occurred", e);
        }

        return BookingResponse.fromEntity(booking);
    }

    private void validateBookingDates(BookingCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate()) || 
            request.getEndDate().isEqual(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private Booking createPendingBooking(Long userId, BookingCreateRequest request, String correlationId) {
        Booking booking = new Booking();
        booking.setRequestId(request.getRequestId());
        booking.setUserId(userId);
        booking.setRoomId(request.getRoomId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(Booking.Status.PENDING);
        booking.setCorrelationId(correlationId);
        booking.setCreatedAt(java.time.OffsetDateTime.now());
        return booking;
    }

    private void processBookingFlow(Booking booking, String correlationId) {
        Map<String, String> holdPayload = Map.of(
                "requestId", booking.getRequestId(),
                "startDate", booking.getStartDate().toString(),
                "endDate", booking.getEndDate().toString()
        );
        
        // Удержание слота (hold)
        callHotel("/rooms/" + booking.getRoomId() + "/hold", holdPayload, correlationId)
                .block(timeout);
        
        // Подтверждение (confirm)
        Map<String, String> confirmPayload = Map.of("requestId", booking.getRequestId());
        callHotel("/rooms/" + booking.getRoomId() + "/confirm", confirmPayload, correlationId)
                .block(timeout);
    }

    private void compensateBooking(Booking booking, String correlationId) {
        try {
            Map<String, String> releasePayload = Map.of("requestId", booking.getRequestId());
            callHotel("/rooms/" + booking.getRoomId() + "/release", releasePayload, correlationId)
                    .block(timeout);
        } catch (Exception e) {
            log.error("[{}] Failed to release room during compensation", correlationId, e);
        }
    }

    private Mono<String> callHotel(String path, Map<String, String> payload, String correlationId) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .header(CORRELATION_ID_HEADER, correlationId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(retries, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2)));
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Подсказки: получить список комнат из Hotel Service и отсортировать
    public record RoomView(Long id, String number, long timesBooked) {}

    public Mono<java.util.List<RoomView>> getRoomSuggestions() {
        return webClient.get()
                .uri("/hotels/rooms")
                .retrieve()
                .bodyToFlux(RoomView.class)
                .collectList()
                .map(list -> list.stream()
                        .sorted(java.util.Comparator.comparingLong(RoomView::timesBooked)
                                .thenComparing(RoomView::id))
                        .toList());
    }
}


