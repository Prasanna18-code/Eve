package com.collegeeventhub.common.util;

import com.collegeeventhub.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private Pagination() {
    }

    public static Pageable pageRequest(int page, int size, Sort sort) {
        validate(page, size);
        return PageRequest.of(page, size, sort);
    }

    public static void validate(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("Page size must be between 1 and 50");
        }
    }
}
