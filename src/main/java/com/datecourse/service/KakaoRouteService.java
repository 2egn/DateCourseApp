package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Live Kakao routes. Responses stay in the current display, not a reusable cache or database. */
public final class KakaoRouteService {
    private static final int MAX_RESPONSE_BYTES=4*1024*1024;
    private static final URI DRIVING=URI.create("https://apis-navi.kakaomobility.com/v1/directions");
    private static final URI TRANSIT=URI.create("https://dapi.kakao.com/v2/routing/publictraffic");
    private final HttpClient client;
    private final URI drivingEndpoint,transitEndpoint;
    private final ObjectMapper json=new ObjectMapper();
    public KakaoRouteService(){this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build(),DRIVING,TRANSIT);}
    KakaoRouteService(HttpClient client,URI driving,URI transit){this.client=client;drivingEndpoint=driving;transitEndpoint=transit;}

    public TravelRoute route(List<Stop> input,TravelMode mode,KakaoRouteSettings settings)throws IOException,InterruptedException {
        var stops=List.copyOf(input);
        if(mode!=TravelMode.DRIVING&&mode!=TravelMode.TRANSIT)throw new IllegalArgumentException("카카오 자동차·대중교통 경로를 선택해 주세요.");
        if(stops.size()<2)throw new IllegalArgumentException("출발 위치와 방문할 장소를 선택해 주세요.");
        if(settings.restApiKey().isEmpty())throw new IOException("카카오 REST API 키가 필요해요. 지도용 JavaScript 키와는 별도예요.");
        var legs=new ArrayList<Leg>();long distance=0,time=0;
        for(int i=1;i<stops.size();i++) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            Stop from=stops.get(i-1),to=stops.get(i);
            Leg leg;
            if(from.lat()==to.lat()&&from.lng()==to.lng())leg=new Leg(from,to,0,0,List.of(),List.of());
            else {
                String query=mode==TravelMode.DRIVING
                        ?"origin="+from.lng()+","+from.lat()+"&destination="+to.lng()+","+to.lat()+"&summary=false&alternatives=false&priority=RECOMMEND"
                        :"start_x="+from.lng()+"&start_y="+from.lat()+"&end_x="+to.lng()+"&end_y="+to.lat()+"&input_coord=WGS84&output_coord=WGS84";
                var body=request(URI.create((mode==TravelMode.DRIVING?drivingEndpoint:transitEndpoint)+"?"+query),settings,mode);
                leg=mode==TravelMode.DRIVING?driving(body,from,to):transit(body,from,to);
            }
            legs.add(leg);distance=Math.addExact(distance,leg.distance());time=Math.addExact(time,leg.time());
        }
        return new TravelRoute(legs,distance,time,mode,"카카오");
    }

    private JsonNode request(URI uri,KakaoRouteSettings settings,TravelMode mode)throws IOException,InterruptedException {
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Authorization","KakaoAK "+settings.restApiKey()).header("Accept","application/json").GET().build();
        HttpResponse<byte[]> response;
        try {response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());}
        catch(HttpTimeoutException error){throw new IOException(mode.label()+" 경로 조회가 지연되고 있어요. 다시 시도해 주세요.");}
        catch(IOException error){throw new IOException("카카오 경로 서비스에 연결하지 못했어요. 네트워크를 확인해 주세요.");}
        {
            if(response.statusCode()!=200) {
                String message=switch(response.statusCode()) {
                    case 401,403->"카카오 REST 키 또는 경로 API 사용 권한을 확인해 주세요.";
                    case 429->"카카오 경로 조회 한도에 도달했어요. 잠시 후 다시 시도해 주세요.";
                    case 400,404->"카카오 "+mode.label()+" 경로를 찾지 못했어요. 선택 위치와 API 사용 설정을 확인해 주세요.";
                    default->"카카오 경로 서비스가 응답하지 않아요. 다시 시도해 주세요.";
                };
                throw new AppDiagnostics.HttpFailure(response.statusCode(),message);
            }
            byte[] bytes=response.body();
            if(bytes.length>MAX_RESPONSE_BYTES)throw new IOException("카카오 경로 응답이 너무 커서 표시하지 못했어요.");
            try {JsonNode value=json.readTree(bytes);if(value==null)throw new IOException();return value;}
            catch(IOException error){throw new IOException("카카오 경로 응답 형식을 확인하지 못했어요.");}
        }
    }

    Leg driving(JsonNode body,Stop from,Stop to)throws IOException {
        try {
            JsonNode selected=null;
            for(var route:array(body.path("routes")))if(route.path("result_code").isIntegralNumber()&&route.path("result_code").intValue()==0){selected=route;break;}
            if(selected==null)throw new IOException("선택 위치 사이의 자동차 경로를 찾지 못했어요.");
            var summary=selected.path("summary");long distance=number(summary,"distance"),time=number(summary,"duration");
            var paths=new ArrayList<List<Coordinate>>();var steps=new ArrayList<Step>();
            for(var section:array(selected.path("sections"))) {
                for(var road:array(section.path("roads"))) {
                    var vertices=array(road.path("vertexes"));if(vertices.size()%2!=0)throw new IllegalArgumentException();
                    var path=new ArrayList<Coordinate>();
                    for(int i=0;i<vertices.size();i+=2)path.add(coordinate(vertices.get(i),vertices.get(i+1)));
                    if(path.size()>1)paths.add(path);
                }
                for(var guide:array(section.path("guides"))) {
                    String text=guide.path("guidance").asText("").strip();
                    if(!text.isEmpty())steps.add(new Step(text,coordinate(guide.path("x"),guide.path("y"))));
                }
            }
            requireGeometry(distance,paths);
            return new Leg(from,to,distance,time,paths,steps);
        }catch(IllegalArgumentException|ArithmeticException error){throw malformed();}
    }

    Leg transit(JsonNode body,Stop from,Stop to)throws IOException {
        if(!"OK".equals(body.path("status").asText()))throw new IOException("선택 위치 사이의 대중교통 경로가 없어요. 도보·자동차 또는 다른 위치를 선택해 주세요.");
        try {
            JsonNode selected=null;long fastest=Long.MAX_VALUE;
            for(var route:array(body.path("routes"))) {
                long time=number(route.path("properties"),"totalTime");
                if(time<fastest){fastest=time;selected=route;}
            }
            if(selected==null)throw malformed();
            var properties=selected.path("properties");
            long distance=number(properties,"totalDistance"),time=number(properties,"totalTime");
            var paths=new ArrayList<List<Coordinate>>();var steps=new ArrayList<Step>();var stages=new ArrayList<TransitStage>();
            for(var stage:array(selected.path("steps"))) {
                var details=stage.path("properties");String type=details.path("type").asText();
                String label=switch(type){case "BUS"->"버스";case "SUBWAY"->"지하철";case "WALKING"->"도보";default->throw new IllegalArgumentException();};
                long seconds=number(details,"time"),meters=number(details,"distance");
                var path=new ArrayList<Coordinate>();var points=stage.path("path").path("points");
                if(!points.isMissingNode()&&!points.isNull())for(var point:array(points)) {
                    if(!point.isArray()||point.size()!=2)throw new IllegalArgumentException();
                    path.add(coordinate(point.get(0),point.get(1)));
                }
                if(path.size()>1)paths.add(path);
                String guidance=details.path("guidance").asText("").strip();
                if(guidance.isEmpty())guidance=label+" 이동";
                stages.add(new TransitStage(type,guidance,meters,seconds,path));
                steps.add(new Step(label+" · "+guidance+" · "+TravelRoute.durationText(seconds),path.isEmpty()?null:path.getFirst()));
            }
            if(stages.isEmpty())throw new IllegalArgumentException();requireGeometry(distance,paths);
            var fare=properties.path("fare");Integer amount=optionalInt(fare,"value"),transfers=optionalInt(properties,"transfers");
            return new Leg(from,to,distance,time,paths,steps,stages,amount,transfers);
        }catch(IllegalArgumentException|ArithmeticException error){throw malformed();}
    }

    private static JsonNode array(JsonNode node) {if(!node.isArray())throw new IllegalArgumentException();return node;}
    private static long number(JsonNode node,String key) {
        var value=node.path(key);if(!value.isIntegralNumber()||!value.canConvertToLong()||value.longValue()<0)throw new IllegalArgumentException();return value.longValue();
    }
    private static Integer optionalInt(JsonNode node,String key) {
        if(node.path(key).isMissingNode()||node.path(key).isNull())return null;
        long value=number(node,key);if(value>Integer.MAX_VALUE)throw new IllegalArgumentException();return (int)value;
    }
    private static Coordinate coordinate(JsonNode x,JsonNode y) {
        if(!x.isNumber()||!y.isNumber())throw new IllegalArgumentException();return new Coordinate(y.doubleValue(),x.doubleValue());
    }
    private static void requireGeometry(long distance,List<List<Coordinate>> paths)throws IOException {
        if(distance>0&&paths.isEmpty())throw new IOException("경로의 상세 좌표를 받지 못했어요. 직선으로 대체하지 않고 다시 조회할 수 있어요.");
    }
    private static IOException malformed(){return new IOException("카카오 경로의 거리·시간·좌표 정보를 확인하지 못했어요.");}
}
