package com.datecourse.model;

public record InformationReport(long id, String placeId, String placeName, String field,
                                String originalValue, String proposedValue, String reason, String sourceUrl,
                                String status, String reviewNote, String createdAt, String reviewedValue) {
    public enum Field {
        PRICE("1인 기준 가격"), SUBCATEGORY("세부 업종"), NAME("장소명"), ADDRESS("주소");
        private final String label;
        Field(String label) { this.label = label; }
        public String label() { return label; }
        @Override public String toString() { return label; }
    }
}
