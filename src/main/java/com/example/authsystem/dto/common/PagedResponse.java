package com.example.authsystem.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response wrapper with metadata")
public class PagedResponse<T> {

    @Schema(description = "Page content elements")
    private List<T> content;

    @Schema(description = "Current zero-based page number", example = "0")
    private int pageNumber;

    @Schema(description = "Number of elements per page", example = "10")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether current page is the last page", example = "false")
    private boolean last;

    @Schema(description = "Whether current page is the first page", example = "true")
    private boolean first;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
