package com.example.springboot.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paged results. Returning Spring Data's Page directly
 * serializes internal PageImpl fields (and Spring Data logs a warning about it),
 * so map it to an explicit DTO.
 */
public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              long totalElements,
                              int totalPages,
                              boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
