# 지도 카메라 움직임 개선 검토 (2026-11-15)

## 결론

출발지 근거리 이동과 반경 조절은 현재 Kakao 지도와 JavaFX WebView 구조에서 개선할 수 있다. 우선 카메라 요청을 한 경계로 모아 직접 이동/자동 맞춤/상세 카드 노출 요청이 충돌하지 않게 한다. 네이티브 애니메이션과 입력 정리를 먼저 적용하고, 사용자 정의 보간은 성능 확인 후 확장한다. 이번 작업은 코드 및 공개 자료 검토이며 구현이나 SDK 실측은 하지 않았다.

## 현재 코드 확인

- map.js의 검색 결과 선택은 selectMapLocation 호출 후 setCenter로 즉시 이동한다.
- ConditionController.updateRange는 반경 변경마다 map.present(...fit=true)를 호출한다. PlannerMap.render → presentRoute → fitRouteView → setBounds로 이어지므로 슬라이더를 움직이는 중에도 전체 카메라를 반복 맞춘다.
- ensureRouteRangesVisible에는 확대/축소의 경계 진동을 줄이는 여유 계산이 있지만 공통 조건의 반복 setBounds 경로와 분리되어 있다.
- 상세 카드 노출은 이미 panTo를 사용한다. 전체 보기, 도보 경로 맞춤은 setBounds를 직접 호출한다. 카메라 이동 정책이 여러 곳에 흩어져 있다.
- instant-zoom.js는 휠 입력 누적과 SDK 300ms 애니메이션, 마지막 요청 대기를 이미 사용한다. 별도 fractional CSS scale 실험도 있지만 현재 비활성화되어 있다.

## 외부 자료에서 확인한 기법

### 네이티브 이동과 결합 이동

Kakao panTo는 가까운 이동을 애니메이션으로 처리하지만 지도 화면 크기보다 큰 이동은 즉시 처리한다. setLevel 애니메이션은 레벨 차이가 2 이하일 때 가능하다고 명시되어 있다. jump(center,level,{animate:{duration}})는 중심과 레벨을 함께 변경하는 방법이다. 큰 이동은 타일 로딩 중 투명 배경이 나타날 수 있다고 설명한다.

- [Kakao Web 문서](https://apis.map.kakao.com/web/documentation/)
- [카카오 개발자 커뮤니티의 setLevel/panTo 충돌 및 jump 안내](https://devtalk.kakao.com/t/setlevel-panto/140208)

현재 SDK 런타임에서 기능을 확인한 뒤 사용한다. setLevel과 panTo를 시간 지연으로 이어붙이는 대신 하나의 목표 중심·레벨로 결합할 수 있는지 시험하는 것이 적합하다.

### 이동 거리의 화면 단위화

같은 1km도 줌 수준에 따라 느껴지는 이동량이 다르다. MapLibre의 screenSpeed는 화면 폭 기준 속도를 사용한다. 우리 코드에서도 미터 고정값보다 지도 투영의 픽셀 이동량을 화면 크기로 나눈 값을 이동 정책에 쓰는 것이 적합하다.

- [MapLibre FlyToOptions](https://maplibre.org/maplibre-gl-js/docs/API/type-aliases/FlyToOptions/)

### 확대·축소를 섞는 비행 경로

D3 interpolateZoom은 중심과 보이는 범위 폭을 함께 보간하고 권장 이동 시간을 계산한다. 긴 이동 중 잠시 축소해 두 위치의 관계를 보여준 뒤 목적지에서 확대하는 식의 전환을 구성할 수 있다. MapLibre/Mapbox 계열의 flyTo와 같은 방향이다. Kakao에 그대로 호출할 수 있는 API는 아니며 지도 투영·정수 줌 단계·타일 렌더링에 맞는 어댑터가 필요하다.

- [D3 zoom interpolation 및 원문 링크](https://d3js.org/d3-interpolate/zoom)
- [MapLibre flyTo 예제](https://maplibre.org/maplibre-gl-js/docs/examples/fly-to-a-location/)

### 중간에 목표가 바뀌는 애니메이션

이동 중 새로운 출발지를 선택하면 이전 목표까지 갔다 돌아오는 느낌을 피해야 한다. Reach/North의 연구는 목표 변경으로 중단될 때의 연속성도 다룬다. 우리 앱에는 먼저 이전 요청을 무효화하고 마지막 목표만 유지하는 정책이 적합하다. 속도까지 매끄럽게 이어가는 spring/동역학 보간은 별도 성능 실험으로 다룬다. Kakao 네이티브 애니메이션의 취소·재지정 동작이 동일하다고 가정하지 않는다.

- [Smooth, Efficient, and Interruptible Zooming and Panning](https://arxiv.org/abs/1801.09358)

### 시각적 보정

transform/opacity 애니메이션은 레이아웃을 매번 바꾸는 방식보다 유리할 수 있다. 지도만 임시 이동·확대해 부드럽게 보이게 한 뒤 SDK 상태를 맞추는 방법은 실험할 만하다. 다만 WebView 안의 타일 선명도, 지도 투영 좌표, SDK의 핀 인식 영역, 경로 오버레이와 실제 SDK 줌 값의 동기화까지 확인해야 한다. 현재 비활성화된 fractional zoom을 단순히 켜는 방식은 우선순위가 낮다.

- [Animations and performance](https://web.dev/articles/animations-and-performance)

## 제안 UX

| 상황 | 제안 |
| --- | --- |
| 가까운 출발지 선택 | 현재 배율 유지, 화면 이동량에 맞춘 짧은 pan 전환 |
| 같은 위치 재선택/작은 변화 | 불필요한 카메라 이동 생략 |
| 반경 드래그 | 범위 표시는 즉시 갱신, 화면을 벗어날 때 필요한 만큼 축소 |
| 반경 드래그 종료 | 최종 범위를 한 번 정리하고, 여유가 충분할 때만 확대 |
| 전체 보기 버튼 | 명시적인 범위 맞춤을 수행하고 가능하면 중심·레벨을 함께 전환 |
| 먼 위치 선택 | 짧은 결합 이동 또는 축소→이동→확대 실험. 과도하게 긴 비행은 피함 |
| 이동 중 재선택 | 이전 목표 요청을 취소/무효화하고 마지막 목표로 전환 |
| 사용자 드래그/휠 개입 | 자동 카메라 요청이 사용자 조작을 덮어쓰지 않음 |
| 동작 줄이기 설정 | 애니메이션을 생략하거나 축소 |

근거리 지속시간 200~350ms, 중간 이동 350~600ms는 초기 실험값으로만 제안한다. 실제 WebView 프레임과 체감으로 조정하며 장소별 예외나 임의의 setTimeout 체인으로 구현하지 않는다.

## 객체 경계

- Java의 조건/코스 객체는 선택 위치·반경·이동 목적을 관리한다.
- MapCameraController는 focusOrigin / fitRange / revealPlace / fitCourse 같은 의도를 받는다.
- CameraMotionPolicy는 현재 화면 기준 이동량·여유·사용자 개입·동작 줄이기 설정으로 목표 상태와 전환 방식을 결정한다.
- KakaoCameraAdapter는 실제 SDK panTo/jump/setLevel 호출 및 지원 여부·애니메이션 종료 상태를 다룬다.
- 입력 병합은 마지막 유효 목표를 유지하며, 프레임 단위 작업이 필요하면 requestAnimationFrame을 사용한다. SDK 네이티브 애니메이션과 사용자 정의 프레임 보간이 동시에 카메라를 제어하지 않게 한다.

처음에는 가까운 이동과 반경 드래그 정책을 작은 범위로 구현하고, 핀 hover/단일 클릭·구간 설명·카드 위치·빠른 재선택 회귀를 실제 SDK에서 확인하는 순서가 적합하다.
