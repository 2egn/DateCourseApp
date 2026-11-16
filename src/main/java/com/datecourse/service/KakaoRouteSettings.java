package com.datecourse.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** REST credential is used only by Java requests, never WebView or saved plans. */
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
