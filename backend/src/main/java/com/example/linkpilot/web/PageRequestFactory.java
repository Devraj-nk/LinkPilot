package com.example.linkpilot.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Clamps client-supplied page/size query params so a list endpoint can't be made to
 * return an unbounded or negative-offset result set.
 */
public final class PageRequestFactory {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable of(int page, int size, String sortField) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortField));
    }
}
