package com.normcat.modules.search.service.impl;

import com.normcat.common.response.PageResponse;
import com.normcat.modules.product.entity.Product;
import com.normcat.modules.product.entity.ProductAttributeValue;
import com.normcat.modules.product.repository.ProductAttributeValueRepository;
import com.normcat.modules.product.repository.ProductRepository;
import com.normcat.modules.search.dto.request.ProductSearchCriteria;
import com.normcat.modules.search.dto.response.ProductSearchResponse;
import com.normcat.modules.search.service.ProductSearchService;
import com.normcat.modules.search.specification.ProductSearchSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSearchResponse> searchProducts(ProductSearchCriteria criteria) {
        log.info("Searching products with criteria: {}", criteria);

        int page = (criteria != null && criteria.getPage() != null && criteria.getPage() >= 0) ? criteria.getPage() : 0;
        int size = (criteria != null && criteria.getSize() != null && criteria.getSize() > 0) ? Math.min(criteria.getSize(), 100) : 20;
        String sortParam = (criteria != null) ? criteria.getSort() : null;

        Sort sort = parseSort(sortParam);
        Pageable pageable = PageRequest.of(page, size, sort);

        ProductSearchSpecification spec = new ProductSearchSpecification(criteria);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        if (productPage.isEmpty()) {
            return PageResponse.of(productPage, Collections.emptyList());
        }

        // Batch fetch canonical attributes to completely avoid N+1 queries
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        List<ProductAttributeValue> pavList = productAttributeValueRepository.findAllWithDetailsByProductIdIn(productIds);

        Map<Long, Map<String, String>> attributesByProductId = new HashMap<>();
        for (ProductAttributeValue pav : pavList) {
            Long prodId = pav.getProduct().getId();
            if (pav.getAttributeValue() != null && pav.getAttributeValue().getAttribute() != null) {
                String attrCode = pav.getAttributeValue().getAttribute().getCode();
                String displayVal = pav.getAttributeValue().getDisplayValue();

                attributesByProductId
                        .computeIfAbsent(prodId, k -> new LinkedHashMap<>())
                        .merge(attrCode, displayVal, (oldVal, newVal) -> oldVal + ", " + newVal);
            }
        }

        List<ProductSearchResponse> content = productPage.getContent().stream()
                .map(product -> ProductSearchResponse.builder()
                        .id(product.getId())
                        .sku(product.getSku())
                        .name(product.getName())
                        .price(product.getPrice())
                        .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                        .normalizationStatus(product.getNormalizationStatus())
                        .normalizedAttributes(attributesByProductId.getOrDefault(product.getId(), Collections.emptyMap()))
                        .build())
                .toList();

        return PageResponse.of(productPage, content);
    }

    private Sort parseSort(String sortParam) {
        if (!StringUtils.hasText(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (property.toLowerCase()) {
            case "price" -> Sort.by(direction, "price");
            case "name" -> Sort.by(direction, "name");
            case "sku" -> Sort.by(direction, "sku");
            case "id" -> Sort.by(direction, "id");
            default -> Sort.by(direction, "createdAt");
        };
    }
}
