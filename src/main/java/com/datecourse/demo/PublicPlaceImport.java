package com.datecourse.demo;

import com.datecourse.service.PublicDemoPlaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.university.localdb.db.MemberRepository;
import kr.ac.university.localdb.db.PlaceCatalogRepository;
import java.time.Instant;
import java.util.Arrays;

/** Preview is offline. Apply uses existing DB settings and only additive source-owned catalog writes. */
public final class PublicPlaceImport {
    public static PlaceCatalogRepository.Batch batch() throws Exception {
        try (var facts = PublicPlaceImport.class.getResourceAsStream(PublicDemoPlaceRepository.FACTS_RESOURCE)) {
            if (facts == null) throw new IllegalStateException("공공 속성 자료가 없습니다.");
            Instant generated = Instant.parse(new ObjectMapper().readTree(facts).path("generatedAt").asText());
            return new PlaceCatalogRepository.Batch("busan-public-places", generated, new PublicDemoPlaceRepository().findAll());
        }
    }
    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--apply")) {
            System.err.println("사용법: PublicPlaceImport [--apply]"); System.exit(2);
        }
        try {
            var batch = batch();
            var prepared = PlaceCatalogRepository.prepare(batch);
            System.out.println("공공 장소 자료: " + batch.places().size() + "곳 · 생성 시각 " + batch.generatedAt());
            System.out.println("자료 지문: " + prepared.fingerprint());
            if (!Arrays.asList(args).contains("--apply")) {
                System.out.println("검증 완료. DB 연결과 쓰기를 수행하지 않았습니다."); return;
            }
            try (var members = new MemberRepository()) {
                var result = members.placeCatalog().synchronize(batch);
                var places = members.places().findAll();
                var effective = members.informationReports().apply(places);
                System.out.println("적재 완료: 신규 " + result.inserted() + ", 갱신 " + result.updated()
                        + ", 재활성 " + result.reactivated() + ", 비활성 " + result.deactivated() + ", 유지 " + result.unchanged());
                System.out.println("운영 조회 " + places.size() + "곳 · 정정 적용 조회 " + effective.size() + "곳 · 적재 이력 " + result.importId());
            }
        } catch (Exception error) {
            // JDBC messages and causes may include private connection details. Do not log them.
            System.err.println("적재 또는 후속 조회 검증을 완료하지 못했습니다. 반영 여부는 적재 이력에서 확인하고 자료 버전·DB 연결·권한을 점검해 주세요.");
            System.exit(1);
        }
    }
}
