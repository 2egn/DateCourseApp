package com.datecourse;

import java.time.LocalDate;

public record Member(
        String memberId,
        String name,
        String role,          // "USER" 또는 "ADMIN"
        LocalDate joinedAt,
        boolean suspended     // true면 정지 상태
) {}