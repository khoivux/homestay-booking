package com.javaweb.app.repository;


import com.javaweb.app.entity.HomestayEntity;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HomestayRepository extends HomestayRepositoryCustom, JpaRepository<HomestayEntity, Long> {
    @NonNull
    List<HomestayEntity> findAll();

    Optional<HomestayEntity> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HomestayEntity h WHERE h.id = :id")
    Optional<HomestayEntity> findByIdWithLock(@Param("id") Long id);

    void deleteByIdIn(List<Long> ids);
    @NotNull
    HomestayEntity getById(Long id);
    List<HomestayEntity> findByIdIn(List<Long> ids);
}
