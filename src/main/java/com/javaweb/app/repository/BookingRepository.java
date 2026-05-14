package com.javaweb.app.repository;

import com.javaweb.app.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    List<BookingEntity> findAll();
    List<BookingEntity> findByUser_Id(Long userId);
    Optional<BookingEntity> findById(Long id);
    List<BookingEntity> findByStatusAndBookingTimeBefore(String status, LocalDateTime expiryTime);
    @Query(value = """
        SELECT COUNT(*)
        FROM booking b
        WHERE b.homestay_id = :homestayId
        AND b.status != 'Đã hủy'
        AND b.status != 'Đã thanh toán'
        AND NOT (
            :checkInDate > b.checkout_date
            OR :checkOutDate < b.checkin_date
        )
        """, nativeQuery = true)
    Long countConflictingBookings(@Param("homestayId") Long homestayId,
                                  @Param("checkInDate") LocalDate checkInDate,
                                  @Param("checkOutDate") LocalDate checkOutDate);
}
