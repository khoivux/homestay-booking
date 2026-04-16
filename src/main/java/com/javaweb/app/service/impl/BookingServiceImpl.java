package com.javaweb.app.service.impl;

import com.javaweb.app.dto.BookingDTO;
import com.javaweb.app.entity.BookingEntity;
import com.javaweb.app.exception.DateNotValidException;
import com.javaweb.app.repository.BookingRepository;
import com.javaweb.app.repository.HomestayRepository;
import com.javaweb.app.repository.UserRepository;
import com.javaweb.app.service.BookingService;
import com.javaweb.app.mapper.BookingMapper;
import org.modelmapper.ModelMapper;
import com.javaweb.app.utils.DateUtil;
import com.javaweb.app.utils.MapUtil;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

import java.util.Map;
import java.util.Objects;

@Service // Thêm annotation @Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    // Inject BookingRepository và BookingMapper
    @Autowired
    public BookingServiceImpl(BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }
    @Autowired
    public HomestayRepository homestayRepository;
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ModelMapper modelMapper;

    public BookingDTO createBooking(Map<String, Object> params,
                                    Long userId,
                                    HttpSession session) {
        BookingDTO bookingDTO = new BookingDTO();
        bookingDTO.setUser(userRepository.getById(userId));
        bookingDTO.setCheckInTime(MapUtil.getObject(params, "checkInTime", Long.class));
        bookingDTO.setCustomerName(MapUtil.getObject(params, "customerName", String.class));
        bookingDTO.setCustomerEmail(MapUtil.getObject(params, "customerEmail", String.class));
        bookingDTO.setCustomerPhone(MapUtil.getObject(params, "customerPhone", String.class));
        bookingDTO.setHomestay(homestayRepository.getById(Objects.requireNonNull(MapUtil.getObject(params, "homestayId", Long.class))));
        bookingDTO.setStatus("Chờ thanh toán");
        bookingDTO.setCheckInDate(DateUtil.strToDate(MapUtil.getObject(params, "checkInDate", String.class)));
        bookingDTO.setCheckOutDate(DateUtil.strToDate(MapUtil.getObject(params, "checkOutDate", String.class)));
        bookingDTO.setBookingTime(LocalDateTime.now());

        long daysBetween = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
        bookingDTO.setStayDuration(daysBetween);
        bookingDTO.setTotal(daysBetween * bookingDTO.getHomestay().getPrice());

        return bookingDTO;
    }
    @Transactional
    public BookingDTO saveBooking(BookingDTO bookingDTO) {
        homestayRepository.findByIdWithLock(bookingDTO.getHomestay().getId())
                .orElseThrow(() -> new RuntimeException("Homestay không tồn tại"));
        validDateBooking(bookingDTO.getHomestay().getId(), bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());

        bookingRepository.save(modelMapper.map(bookingDTO, BookingEntity.class));
        return bookingDTO;
    }
    public void validDateBooking(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        Long conflictingBookings = bookingRepository.countConflictingBookings(homestayId, checkInDate, checkOutDate);
        if (conflictingBookings > 0) {
            throw new DateNotValidException("Homestay không sẵn có trong thời gian này!");
        }
    }
    public List<BookingDTO> getPaymentHistory(Long userId) {
        List<BookingEntity> bookings = bookingRepository.findByUser_Id(userId);
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }
        return bookings.stream()
                .map(bookingMapper::mapToBookingDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDTO> findBookingByUserId(Long userId)
    {
        List<BookingDTO> bookingDTOS = new ArrayList<>();
        List<BookingEntity> bookingEntitys = bookingRepository.findByUser_Id(userId);

        for (BookingEntity bookingEntity : bookingEntitys)
        {
            bookingDTOS.add(modelMapper.map(bookingEntity, BookingDTO.class));
        }
        return bookingDTOS;
    }

    @Override
    public void deleteBookingById(Long id) {
        bookingRepository.deleteById(id);
    }

    public List<BookingDTO> getBookingsByUser_Id(Long userId) {
        List<BookingEntity> bookings = bookingRepository.findByUser_Id(userId);
        // Chuyển đổi BookingEntity thành BookingDTO
        return bookings.stream()
                .map(booking -> bookingMapper.mapToBookingDTO(booking)) // Giả sử bạn có mapper
                .collect(Collectors.toList());
    }
    @Override
    public BookingDTO findById(Long id)
    {
        BookingDTO bookingDTO = modelMapper.map(bookingRepository.getById(id), BookingDTO.class);
        return bookingDTO;
    }

    @Override
    public void updateBookingStatus(Long id, String status) {
        BookingEntity booking = bookingRepository.findById(id).orElse(null);
        if (booking != null) {
            booking.setStatus(status);
            bookingRepository.save(booking);
        }
    }
    public void cancelBookingById(Long id) {
        BookingEntity bookingEntity = bookingRepository.getById(id);
        bookingEntity.setStatus("Đã hủy");
        bookingRepository.save(bookingEntity);
    }

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(5);
        List<BookingEntity> expiredBookings = bookingRepository.findByStatusAndBookingTimeBefore("Chờ thanh toán", expiryTime);
        
        for (BookingEntity booking : expiredBookings) {
            booking.setStatus("Đã hủy");
            bookingRepository.save(booking);
            System.out.println("Đã hủy booking ID: " + booking.getId());
        }
    }
}
