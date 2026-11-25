package com.datecourse.db;
import com.datecourse.demo.PublicPlaceImport;
import com.datecourse.model.*;
import com.datecourse.service.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
/** 기존 장소·조건으로 후보 다섯 곳의 연결 가능성을 비교하는 오프라인 탐색 도구다. */
public final class RouteResourceProbe {
 private static final NextPlaceService SEARCH=new NextPlaceService();
 private static final List<String> ACTIVITIES=List.of("식사","카페","문화");
 public static void main(String[] args)throws Exception {
  if(args.length!=5)throw new IllegalArgumentException("장소·속성·모의 결과·출발지·출력 경로를 지정해 주세요.");
  validateOutput(args);
  var json=new ObjectMapper();
  var places=PublicPlaceImport.batch(Path.of(args[0]),Path.of(args[1])).places();
  var report=json.readTree(Path.of(args[2]).toFile());
  if(report.path("expandedPlaces").asInt()!=places.size())throw new IllegalArgumentException("장소 수와 모의 결과가 다릅니다.");
  var origins=json.readTree(Path.of(args[3]).toFile()).path("origins");var coords=new HashMap<String,JsonNode>();origins.forEach(o->coords.put(o.path("name").asText(),o));
  int tested=0,rescued=0;var examples=new ArrayList<Map<String,Object>>();long started=System.nanoTime();
  for(var row:report.path("scenarios")) {
   if(row.path("afterVisited").asInt()==3)continue;tested++;
   var origin=coords.get(row.path("origin").asText());var found=find(places,new ArrayList<>(),origin.path("lat").asDouble(),origin.path("lng").asDouble(),row);
   if(found!=null){rescued++;if(examples.size()<8){var e=new LinkedHashMap<String,Object>();e.put("origin",row.path("origin").asText());e.put("mode",row.path("mode").asText());e.put("radius",row.path("radius").asInt());e.put("legRadius",row.path("legRadius").asInt());e.put("priceCap",row.path("priceCap").asInt());e.put("includeUnknown",row.path("includeUnknown").asBoolean());e.put("greedyVisited",row.path("afterVisited").asInt());e.put("alternative",found.stream().map(Place::stableKey).toList());examples.add(e);}}
  }
  var output=new LinkedHashMap<String,Object>();output.put("scope","Exploratory bounded backtracking through at most the nearest five eligible candidates per stage; offline, fixed meal-cafe-culture sequence, no route APIs or DB; not production behavior, real travel time or user completion rate");output.put("inputs",inputChecksums(args));output.put("candidateLimitPerStage",5);output.put("scenarioCount",report.path("scenarioCount").asInt());output.put("greedyCompleted",report.path("completedAfter").asInt());output.put("testedIncompleteScenarios",tested);output.put("rescuedIncompleteScenarios",rescued);output.put("exploratoryCompleted",report.path("completedAfter").asInt()+rescued);output.put("examples",examples);output.put("elapsedSeconds",(System.nanoTime()-started)/1e9);
  Path target=Path.of(args[4]);if(target.getParent()!=null)Files.createDirectories(target.getParent());json.writerWithDefaultPrettyPrinter().writeValue(target.toFile(),output);System.out.println(json.writeValueAsString(output));
 }
 /** 사용자 조건을 완화하지 않고 각 단계의 가까운 후보 다섯 곳까지만 탐색한다. */
 private static List<Place> find(List<Place> places,ArrayList<Place> confirmed,double lat,double lng,JsonNode row){
  if(confirmed.size()==ACTIVITIES.size())return List.copyOf(confirmed);
  var condition=new NextPlaceCondition(ACTIVITIES.get(confirmed.size()),row.path("priceCap").asInt(),row.path("legRadius").asInt(),row.path("includeUnknown").asBoolean(),Set.of(),Set.of(),5);
  var candidates=SEARCH.candidates(places,confirmed,lat,lng,2,row.path("radius").asInt(),condition);
  for(var candidate:candidates){confirmed.add(candidate.place());var found=find(places,confirmed,lat,lng,row);confirmed.remove(confirmed.size()-1);if(found!=null)return found;}
  return null;
 }
 /** 파일 쌍·모의 결과·출발지의 지문을 남겨 탐색 입력을 구분한다. */
 private static Map<String,String> inputChecksums(String[] args)throws Exception {
  var result=new LinkedHashMap<String,String>();
  for(int i=0;i<4;i++)result.put(Path.of(args[i]).toString().replace('\\','/'),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[i])))));
  return result;
 }

 /** 재현 보고서가 입력 자료를 덮어쓰지 않도록 확인한다. */
 private static void validateOutput(String[] args)throws Exception {
  var target=Path.of(args[4]).toAbsolutePath().normalize();
  for(int i=0;i<4;i++) {
   var input=Path.of(args[i]).toAbsolutePath().normalize();
   if(target.equals(input)||Files.exists(target)&&Files.isSameFile(target,input))
    throw new IllegalArgumentException("보고서 경로로 입력 자료를 덮어쓸 수 없습니다.");
  }
 }

}
