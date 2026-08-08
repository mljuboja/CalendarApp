package com.calendarapp.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.calendarapp.dto.CategoryRequest;
import com.calendarapp.dto.CategoryResponse;
import com.calendarapp.entity.User;
import com.calendarapp.service.CategoryService;

// Category CRUD, scoped to whichever user the JWT identifies. Requires a valid
// JWT - see JwtAuthenticationFilter and SecurityConfig.
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CategoryResponse response = categoryService.createCategory(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CategoryResponse> listCategories(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return categoryService.listCategories(user.getId());
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return categoryService.getCategory(id, user.getId());
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return categoryService.updateCategory(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        categoryService.deleteCategory(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
