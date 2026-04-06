package com.itnews.backend.news;

import java.util.List;

public record NewsPageResponse(
        List<NewsDto> content,
        int totalPages,
        int currentPage,
        long totalElements
) {}
