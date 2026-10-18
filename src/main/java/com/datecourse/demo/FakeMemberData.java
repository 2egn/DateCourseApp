package com.datecourse.demo;

import com.datecourse.model.Member;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 백엔드 연동 전까지 쓰는 가짜 회원
public class FakeMemberData {

    public static List<Member> getMembers() {
        List<Member> list = new ArrayList<>();
        list.add(new Member("m1", "민지", "USER", LocalDate.of(2026, 9, 1), false));
        list.add(new Member("m2", "준호", "USER", LocalDate.of(2026, 9, 3), false));
        list.add(new Member("m3", "서연", "USER", LocalDate.of(2026, 9, 10), false));
        list.add(new Member("m4", "하늘", "USER", LocalDate.of(2026, 9, 15), false));
        list.add(new Member("m5", "도윤", "USER", LocalDate.of(2026, 9, 20), true));
        list.add(new Member("m0", "관리자", "ADMIN", LocalDate.of(2026, 8, 20), false));
        return list;
    }
}