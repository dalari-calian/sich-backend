package com.sich.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.category.dto.CategoryRequest;
import com.sich.category.dto.CategoryResponse;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ResourceAlreadyExistsException("Já existe uma categoria com esse nome");
        }
        CategoryEntity category = new CategoryEntity();
        category.setName(request.name());
        category.setDescription(request.description());
        return CategoryResponse.of(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        CategoryEntity category = findEntityById(id);
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResourceAlreadyExistsException("Já existe uma categoria com esse nome");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        return CategoryResponse.of(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        CategoryEntity category = findEntityById(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Não é possível excluir uma categoria que possui produtos");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.of(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryEntity findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }
}
