package com.calendarapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.calendarapp.dto.CategoryRequest;
import com.calendarapp.dto.CategoryResponse;
import com.calendarapp.entity.Category;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CategoryNotFoundException;
import com.calendarapp.repository.CategoryRepository;

// Handles category CRUD for the authenticated user. Every read/update/delete looks
// up the category by (id, ownerId) together, so a user can never touch a category
// that isn't theirs.
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Owner is taken from the authenticated principal, never from the request.
    public CategoryResponse createCategory(CategoryRequest request, User owner) {
        Category category = new Category();
        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setOwner(owner);

        Category savedCategory = categoryRepository.save(category);
        return toResponse(savedCategory);
    }

    public List<CategoryResponse> listCategories(Long ownerId) {
        return categoryRepository.findByOwnerId(ownerId).stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    public CategoryResponse getCategory(Long categoryId, Long ownerId) {
        Category category = findOwnedCategory(categoryId, ownerId);
        return toResponse(category);
    }

    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request, Long ownerId) {
        Category category = findOwnedCategory(categoryId, ownerId);
        category.setName(request.getName());
        category.setColor(request.getColor());

        Category savedCategory = categoryRepository.save(category);
        return toResponse(savedCategory);
    }

    public void deleteCategory(Long categoryId, Long ownerId) {
        Category category = findOwnedCategory(categoryId, ownerId);
        categoryRepository.delete(category);
    }

    // Shared by get/update/delete: looks up a category scoped to its owner, or
    // throws CategoryNotFoundException if it doesn't exist or belongs to someone else.
    private Category findOwnedCategory(Long categoryId, Long ownerId) {
        return categoryRepository.findByIdAndOwnerId(categoryId, ownerId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
}
