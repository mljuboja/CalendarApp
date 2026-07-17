package com.calendarapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.calendarapp.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwnerId(Long ownerId);
}
