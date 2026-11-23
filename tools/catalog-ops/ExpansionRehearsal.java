package kr.ac.university.localdb.db;

import com.datecourse.demo.PublicPlaceImport;
import com.datecourse.model.*;
import com.datecourse.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** 실제 Java 추천·범위 검사를 사용하며 공급사 호출과 DB 연결은 수행하지 않는다. */
public final class ExpansionRehearsal {
    private static final ObjectMapper JSON=new ObjectMapper();
    public static void main(String[] args) throws Exception {
        boolean priceUpdate=args.length==7 && args[6].equals("price-update");
        var before=PublicPlaceImport.batch(Path.of(args[0]),Path.of(args[1])).places();
        var after=PublicPlaceImport.batch(Path.of(args[2]),Path.of(args[3])).places();
        var origins=JSON.readTree(Path.of(args[4]).toFile()).path("origins");
        var rows=new ArrayList<Map<String,Object>>();var coverage=new ArrayList<Map<String,Object>>();
        var service=new NextPlaceService();var validator=new RouteRangeValidator();
        for(var origin:origins) {
            double lat=origin.path("lat").asDouble(),lng=origin.path("lng").asDouble();
            for(var mode:TravelMode.values()) for(int scale:new int[]{1,2}) for(int cap:new int[]{10000,20000}) for(boolean unknown:new boolean[]{false,true}) {
                int radius=mode==TravelMode.WALKING?scale*1000:mode==TravelMode.DRIVING?scale*10000:scale*5000;
                int legRadius=mode==TravelMode.WALKING?scale*500:mode==TravelMode.DRIVING?scale*3000:scale*1500;
                int[] visited=new int[2];
                List<Place> baselineRoute=List.of(),expandedRoute=List.of();
                for(int pass=0;pass<2;pass++) {
                    var places=pass==0?before:after;var confirmed=new ArrayList<Place>();var radii=new HashMap<Long,Integer>();
                    for(String activity:List.of("식사","카페","문화")) {
                        var condition=new NextPlaceCondition(activity,cap,legRadius,unknown,Set.of(),Set.of(),30);
                        var candidates=service.candidates(places,confirmed,lat,lng,2,radius,condition);
                        check(candidates.size()<=30,"DISPLAY_LIMIT");
                        for(var candidate:candidates) {
                            check(confirmed.stream().noneMatch(p->p.sameIdentityAs(candidate.place())),"DUPLICATE_CANDIDATE");
                            check(unknown||candidate.place().priceEstimate().withinCap(cap),"PRICE_CONDITION");
                        }
                        if(candidates.isEmpty())break;
                        var selected=candidates.getFirst().place();confirmed.add(selected);radii.put(selected.recordId(),legRadius);
                        var plan=new RoutePlan(lat,lng,2,radius,"offline",confirmed,radii,mode);
                        check(validator.outsideRange(plan).isEmpty(),"GLOBAL_RADIUS");
                        check(validator.legs(plan).stream().noneMatch(RouteRangeValidator.Leg::exceeded),"LEG_RADIUS");
                        check(plan.costEstimate().unknownPlaces()>=0,"PRICE_MODEL");
                    }
                    visited[pass]=confirmed.size();
                    if(pass==0)baselineRoute=List.copyOf(confirmed);else expandedRoute=List.copyOf(confirmed);
                }
                var replay=new ArrayList<Place>();
                var activities=List.of("식사","카페","문화");
                boolean baselineEligible=true;
                for(int step=0;step<baselineRoute.size();step++) {
                    var expected=baselineRoute.get(step);
                    var condition=new NextPlaceCondition(activities.get(step),cap,legRadius,unknown,Set.of(),Set.of(),30);
                    boolean eligible=service.matching(after,replay,lat,lng,2,radius,condition).stream().anyMatch(p->p.place().sameIdentityAs(expected));
                    baselineEligible &= eligible;
                    if(!priceUpdate)check(eligible,"BASELINE_ROUTE_LOST");
                    replay.add(expected);
                }
                var row=new LinkedHashMap<String,Object>(Map.of("origin",origin.path("name").asText(),"mode",mode.name(),"radius",radius,"legRadius",legRadius,
                    "priceCap",cap,"includeUnknown",unknown,"beforeVisited",visited[0],"afterVisited",visited[1]));
                row.put("baselineRouteStillEligible",baselineEligible);
                row.put("beforeRoute",baselineRoute.stream().map(Place::stableKey).toList());
                row.put("afterRoute",expandedRoute.stream().map(Place::stableKey).toList());
                rows.add(row);
            }
            for(String activity:List.of("식사","카페","문화")) {
                var condition=new NextPlaceCondition(activity,20000,1000,true,Set.of(),Set.of(),30);
                int old=service.matching(before,List.of(),lat,lng,2,2000,condition).size();
                int now=service.matching(after,List.of(),lat,lng,2,2000,condition).size();
                check(now>=old,"FIXED_ORIGIN_COVERAGE_REGRESSED");
                coverage.add(Map.of("origin",origin.path("name").asText(),"activity",activity,"before",old,"after",now));
                for(var filter:PlacePriceFilter.values()) for(var tag:List.of(PlaceTag.NEAR_BUS,PlaceTag.NEAR_SUBWAY)) {
                    var specific=new NextPlaceCondition(activity,20000,2000,true,Set.of(),Set.of(tag),30,Set.of(),filter);
                    for(var candidate:service.candidates(after,List.of(),lat,lng,2,2000,specific)) {
                        check(candidate.place().details().hasTag(tag),"TAG_FILTER");
                        check(filter.accepts(candidate.place().priceEstimate()),"PRICE_FILTER");
                    }
                }
            }
        }
        var report=new LinkedHashMap<String,Object>();report.put("method","Nearest eligible venue, no backtracking; offline candidate/range checks, not a user completion rate or real travel-route comparison");
        report.put("baselinePlaces",before.size());report.put("expandedPlaces",after.size());report.put("scenarioCount",rows.size());
        report.put("completedBefore",rows.stream().filter(r->r.get("beforeVisited").equals(3)).count());
        report.put("completedAfter",rows.stream().filter(r->r.get("afterVisited").equals(3)).count());
        report.put("priceUpdate",priceUpdate);
        report.put("baselineIneligibleScenarios",rows.stream().filter(r->Boolean.FALSE.equals(r.get("baselineRouteStillEligible"))).count());
        report.put("greedyRegressions",rows.stream().filter(r->(int)r.get("afterVisited")<(int)r.get("beforeVisited")).toList());
        report.put("fixedOriginCoverage",coverage);report.put("scenarios",rows);
        Files.writeString(Path.of(args[5]),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report).replace("\r\n","\n")+"\n");
        System.out.println("Scenarios="+rows.size()+" completedBefore="+report.get("completedBefore")+" completedAfter="+report.get("completedAfter")+" greedyRegressions="+((List<?>)report.get("greedyRegressions")).size());
    }
    private static void check(boolean condition,String code){if(!condition)throw new IllegalStateException(code);}
}
