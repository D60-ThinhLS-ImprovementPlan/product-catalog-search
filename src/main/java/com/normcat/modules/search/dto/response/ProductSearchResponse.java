package com.normcat.modules.search.dto.response;

import com.normcat.modules.product.entity.ProductNormalizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product search response item with flattened canonical attributes")
public class ProductSearchResponse {

    @Schema(description = "Product ID", example = "101")
    private Long id;

    @Schema(description = "Product unique SKU", example = "TSHIRT-RED-XL")
    private String sku;

    @Schema(description = "Product Name", example = "Áo Thun Cotton Cổ Tròn")
    private String name;

    @Schema(description = "Product Price", example = "199000.00")
    private BigDecimal price;

    @Schema(description = "Category Name", example = "Áo sơ mi nam")
    private String categoryName;

    @Schema(description = "Product Normalization Status", example = "NORMALIZED")
    private ProductNormalizationStatus normalizationStatus;

    @Builder.Default
    @Schema(description = "Map of canonical attribute code to display value", example = "{\"COLOR\": \"Đỏ\", \"SIZE\": \"XL\"}")
    private Map<String, String> normalizedAttributes = new LinkedHashMap<>();
}
