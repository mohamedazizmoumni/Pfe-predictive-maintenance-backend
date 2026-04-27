package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.CategoryRequest;
import com.pfe.predictive.inventory.dto.CategoryResponse;
import com.pfe.predictive.inventory.entity.Category;
import com.pfe.predictive.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName() == null ? null : request.getName().trim();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }

        Category cat = Category.builder().name(name).build();
        Category saved = categoryRepository.save(cat);
        return new CategoryResponse(saved.getId(), saved.getName());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
}
