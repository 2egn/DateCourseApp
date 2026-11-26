package com.datecourse.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * - 카카오 경로 요청에 사용할 REST 키와 설정 로드용 클래스
 * - REST 키는 Java HTTP 요청에만 사용, WebView나 저장 계획에 전달하지 않음
 */
public record KakaoRouteSettings(String restApiKey) {
    public static final Path LOCAL_FILE=Path.of(".local","kakao-routing.properties");
    public KakaoRouteSettings {
        restApiKey=restApiKey==null?"":restApiKey.trim();
        if(!restApiKey.isEmpty()&&!restApiKey.matches("[A-Za-z0-9_-]{16,256}"))
            throw new IllegalArgumentException("카카오 REST API 키 형식을 확인해 주세요.");
    }
    @Override public String toString(){return "KakaoRouteSettings[credential=redacted]";}
    public static KakaoRouteSettings load() throws IOException {
        return load(Path.of(""),System.getenv());
    }
    static KakaoRouteSettings load(Path root,Map<String,String> environment) throws IOException {
        String key=environment.getOrDefault("KAKAO_REST_API_KEY","").trim();
        if(key.isEmpty()&&Files.exists(root.resolve(LOCAL_FILE))) {
            var settings=new Properties();try(var input=Files.newInputStream(root.resolve(LOCAL_FILE))){settings.load(input);}
            key=settings.getProperty("kakao.rest.api.key","").trim();
        }
        if(key.isEmpty())key=TeamApiKeys.get("kakaoRest",root,environment);
        return new KakaoRouteSettings(key);
    }
}
