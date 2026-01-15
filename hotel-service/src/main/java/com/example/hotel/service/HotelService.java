package com.example.hotel.service;

import com.example.hotel.dto.*;
import com.example.hotel.exception.ResourceNotFoundException;
import com.example.hotel.exception.RoomUnavailableException;
import com.example.hotel.model.Hotel;
import com.example.hotel.model.Room;
import com.example.hotel.model.RoomReservationLock;
import com.example.hotel.repo.HotelRepository;
import com.example.hotel.repo.RoomRepository;
import com.example.hotel.repo.RoomReservationLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationLockRepository lockRepository;

    public HotelService(HotelRepository hotelRepository, 
                       RoomRepository roomRepository, 
                       RoomReservationLockRepository lockRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.lockRepository = lockRepository;
    }

    // Hotel CRUD operations
    public List<HotelResponse> listHotels() {
        return hotelRepository.findAll().stream()
                .map(HotelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public HotelResponse getHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return HotelResponse.fromEntity(hotel);
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setAddress(request.getAddress());
        
        Hotel savedHotel = hotelRepository.save(hotel);
        return HotelResponse.fromEntity(savedHotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setAddress(request.getAddress());
        
        Hotel updatedHotel = hotelRepository.save(hotel);
        return HotelResponse.fromEntity(updatedHotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
    }

    // Room CRUD operations
    public List<RoomResponse> listRooms() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public RoomResponse getRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        return RoomResponse.fromEntity(room);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Room room = new Room();
        room.setNumber(request.getNumber());
        room.setCapacity(request.getCapacity());
        room.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        
        if (request.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + request.getHotelId()));
            room.setHotel(hotel);
        }
        
        Room savedRoom = roomRepository.save(room);
        return RoomResponse.fromEntity(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        
        room.setNumber(request.getNumber());
        room.setCapacity(request.getCapacity());
        room.setAvailable(request.getAvailable() != null ? request.getAvailable() : room.isAvailable());
        
        if (request.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + request.getHotelId()));
            room.setHotel(hotel);
        }
        
        Room updatedRoom = roomRepository.save(room);
        return RoomResponse.fromEntity(updatedRoom);
    }

    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    // Room availability: hold/confirm/release with idempotency by requestId
    @Transactional
    public RoomLockResponse holdRoom(RoomHoldRequest request, Long roomId) {
        // Check for existing hold with same requestId (idempotency)
        return lockRepository.findByRequestId(request.getRequestId())
                .map(RoomLockResponse::fromEntity)
                .orElseGet(() -> createNewHold(request, roomId));
    }

    private RoomLockResponse createNewHold(RoomHoldRequest request, Long roomId) {
        // Verify room exists
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
        
        // Check for conflicting holds or confirmations
        List<RoomReservationLock> conflicts = lockRepository
                .findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        roomId,
                        Arrays.asList(RoomReservationLock.Status.HELD, RoomReservationLock.Status.CONFIRMED),
                        request.getEndDate(),
                        request.getStartDate()
                );
        
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the specified dates");
        }
        
        RoomReservationLock lock = new RoomReservationLock();
        lock.setRequestId(request.getRequestId());
        lock.setRoomId(roomId);
        lock.setStartDate(request.getStartDate());
        lock.setEndDate(request.getEndDate());
        lock.setStatus(RoomReservationLock.Status.HELD);
        
        RoomReservationLock savedLock = lockRepository.save(lock);
        return RoomLockResponse.fromEntity(savedLock);
    }

    @Transactional
    public RoomLockResponse confirmHold(RoomActionRequest request) {
        RoomReservationLock lock = lockRepository.findByRequestId(request.getRequestId())
                .orElseThrow(() -> new IllegalStateException("Hold not found for requestId: " + request.getRequestId()));
        
        // Idempotency: if already confirmed, return existing
        if (lock.getStatus() == RoomReservationLock.Status.CONFIRMED) {
            return RoomLockResponse.fromEntity(lock);
        }
        
        if (lock.getStatus() == RoomReservationLock.Status.RELEASED) {
            throw new IllegalStateException("Cannot confirm a released hold");
        }
        
        lock.setStatus(RoomReservationLock.Status.CONFIRMED);
        
        // Increment booking counter for statistics
        roomRepository.findById(lock.getRoomId()).ifPresent(room -> {
            room.setTimesBooked(room.getTimesBooked() + 1);
            roomRepository.save(room);
        });
        
        RoomReservationLock confirmedLock = lockRepository.save(lock);
        return RoomLockResponse.fromEntity(confirmedLock);
    }

    @Transactional
    public RoomLockResponse releaseHold(RoomActionRequest request) {
        RoomReservationLock lock = lockRepository.findByRequestId(request.getRequestId())
                .orElseThrow(() -> new IllegalStateException("Hold not found for requestId: " + request.getRequestId()));
        
        // Idempotency: if already released, return existing
        if (lock.getStatus() == RoomReservationLock.Status.RELEASED) {
            return RoomLockResponse.fromEntity(lock);
        }
        
        // If already confirmed, don't release (for idempotency of compensation logic)
        if (lock.getStatus() == RoomReservationLock.Status.CONFIRMED) {
            return RoomLockResponse.fromEntity(lock);
        }
        
        lock.setStatus(RoomReservationLock.Status.RELEASED);
        RoomReservationLock releasedLock = lockRepository.save(lock);
        return RoomLockResponse.fromEntity(releasedLock);
    }
}


