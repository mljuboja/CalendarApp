package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.calendarapp.dto.CategoryRequest;
import com.calendarapp.dto.CategoryResponse;
import com.calendarapp.entity.Category;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CategoryNotFoundException;
import com.calendarapp.repository.CategoryRepository;

// Tests for CategoryService. CategoryRepository is mocked so these don't need a
// real database - ownership is enforced by scoping every lookup by owner ID.
class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void createCategoryAssignsAuthenticatedUserAsOwner() {
        User owner = new User();
        owner.setId(1L);
        CategoryRequest request = new CategoryRequest("Work", "#4A90E2");

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(10L);
            return category;
        });

        CategoryResponse response = categoryService.createCategory(request, owner);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Work");
        assertThat(response.getColor()).isEqualTo("#4A90E2");

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void listCategoriesReturnsOnlyCategoriesForThatUser() {
        Category category1 = categoryWith(1L, "Work", "#4A90E2");
        Category category2 = categoryWith(2L, "Personal", "#FF5733");

        when(categoryRepository.findByOwnerId(1L)).thenReturn(List.of(category1, category2));

        List<CategoryResponse> responses = categoryService.listCategories(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::getName).containsExactly("Work", "Personal");
        verify(categoryRepository).findByOwnerId(1L);
    }

    @Test
    void getOwnedCategorySucceeds() {
        Category category = categoryWith(5L, "Work", "#4A90E2");
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategory(5L, 1L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Work");
    }

    @Test
    void getAnotherUsersCategoryThrowsCategoryNotFoundException() {
        when(categoryRepository.findByIdAndOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(5L, 2L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updateCategoryChangesNameAndColor() {
        Category category = categoryWith(5L, "Work", "#4A90E2");
        CategoryRequest request = new CategoryRequest("Renamed", "#00FF00");

        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(5L, request, 1L);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getColor()).isEqualTo("#00FF00");
    }

    @Test
    void deleteOwnedCategoryCallsRepositoryDelete() {
        Category category = categoryWith(5L, "Work", "#4A90E2");
        when(categoryRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(5L, 1L);

        verify(categoryRepository).delete(category);
    }

    private static Category categoryWith(Long id, String name, String color) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setColor(color);
        return category;
    }
}
