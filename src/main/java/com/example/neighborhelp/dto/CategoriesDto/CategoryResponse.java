package com.example.neighborhelp.dto.CategoriesDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO for category response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Long resourceCount; // Number of resources in this category
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
