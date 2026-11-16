package com.datecourse.demo;

import com.datecourse.model.TravelMode;
import com.datecourse.model.TravelRoute.Stop;
import com.datecourse.service.*;
import java.util.List;

/** Opt-in public-coordinate connectivity check. Does not retain provider responses or user records. */
public final class KakaoRouteProbe {
    public static void main(String[] args) {
        try {
            var settings=KakaoRouteSettings.load();
            if(settings.restApiKey().isBlank()){System.err.println("카카오 REST API 키를 설정한 뒤 다시 실행하세요.");System.exit(2);}
            var service=new KakaoRouteService();
            var points=List.of(new Stop(35.153412,129.118511,"광안공원"),new Stop(35.16578,129.114842,"수영역"),new Stop(35.155489,129.134419,"수변공원"));
            boolean failed=false;
            for(var mode:List.of(TravelMode.DRIVING,TravelMode.TRANSIT)) {
                try {
                    var route=service.route(points,mode,settings);
                    if(route.legs().size()!=2||route.totalDistance()<=0||route.totalTime()<=0)throw new IllegalStateException();
                    System.out.println(mode.name()+": OK · 2개 구간 좌표·시간 수신 확인 (응답 미보관)");
                }catch(Exception failure) {
                    System.err.println(mode.name()+": FAIL · "+failure.getClass().getSimpleName()+" · "+(failure instanceof java.io.IOException?failure.getMessage():"경로 구조를 확인하세요."));failed=true;
                }
            }
            if(failed)System.exit(1);
        }catch(Exception failure){System.err.println("카카오 경로 설정을 확인하지 못했습니다. "+failure.getClass().getSimpleName());System.exit(1);}
    }
}
