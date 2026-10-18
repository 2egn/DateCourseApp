package com.datecourse.model;

import java.util.List;

public record SearchCondition(
        int budget,
        int people,
        List<String> categories,
        double lat,
        double lng,
        int radius
) {}