(function () {
    'use strict';
    const INITIAL = {lat:35.1578, lng:129.06, radius:500, level:5};
    const FIT = {padding:42, minLevel:1, maxLevel:14, negligiblePixel:1};
    let map, circle, marker, ready=false, moving=false, pending=null;
    const notice=document.getElementById('camera-notice');
    function report(message) {
        if(window.cameraDemoBridge)window.cameraDemoBridge.onStatus(message);
    }
    function coordinates(lat,lng) {return new kakao.maps.LatLng(lat,lng);}
    function boundary(lat,lng,radius) {
        const latitudeSpan=radius/6378137*180/Math.PI;
        const longitudeSpan=latitudeSpan/Math.cos(lat*Math.PI/180);
        const bounds=new kakao.maps.LatLngBounds();
        bounds.extend(coordinates(lat-latitudeSpan,lng-longitudeSpan));
        bounds.extend(coordinates(lat+latitudeSpan,lng+longitudeSpan));return bounds;
    }
    function draw(lat,lng,radius) {
        const point=coordinates(lat,lng);marker.setPosition(point);circle.setPosition(point);circle.setRadius(radius);
    }
    function measure(lat,lng,radius,reduce) {
        const viewport=document.getElementById('camera-map'),projection=map.getProjection();
        const point=projection.containerPointFromCoords(coordinates(lat,lng));
        const bounds=boundary(lat,lng,radius),low=projection.containerPointFromCoords(bounds.getSouthWest()),high=projection.containerPointFromCoords(bounds.getNorthEast());
        const halfW=Math.max(1,viewport.clientWidth/2-FIT.padding),halfH=Math.max(1,viewport.clientHeight/2-FIT.padding);
        const fitRatio=Math.max(Math.abs(high.x-low.x)/2/halfW,Math.abs(high.y-low.y)/2/halfH);
        const overflowRatio=Math.max(Math.abs(low.x-viewport.clientWidth/2)/halfW,Math.abs(high.x-viewport.clientWidth/2)/halfW,
            Math.abs(low.y-viewport.clientHeight/2)/halfH,Math.abs(high.y-viewport.clientHeight/2)/halfH);
        const fitLevel=Math.max(FIT.minLevel,Math.min(FIT.maxLevel,map.getLevel()+Math.ceil(Math.log(Math.max(fitRatio,1e-9))/Math.LN2)));
        return {level:map.getLevel(),fitLevel:fitLevel,screenDistance:Math.hypot(point.x-viewport.clientWidth/2,point.y-viewport.clientHeight/2)
            /Math.max(1,Math.min(viewport.clientWidth,viewport.clientHeight)),overflowRatio:overflowRatio,
            reducedMotion:!!reduce || window.matchMedia('(prefers-reduced-motion: reduce)').matches};
    }
    function cancel() {pending=null;moving=false;report('직접 조작 · 자동 이동 대기 취소');}
    function start(command) {
        const point=coordinates(command.lat,command.lng);
        const projected=map.getProjection().containerPointFromCoords(point),viewport=document.getElementById('camera-map');
        if(command.movement==='NONE')return;
        if(command.movement==='BASELINE_FOCUS') {
            map.setCenter(point);report('출발지 즉시 이동');return;
        }
        if(command.movement==='BASELINE') {
            map.setBounds(boundary(command.lat,command.lng,command.radius),FIT.padding,FIT.padding,FIT.padding,FIT.padding);
            report('화면 즉시 맞춤');return;
        }
        if(command.movement==='IMMEDIATE') {
            if(typeof map.jump==='function')map.jump(point,command.level,{animate:false});
            else {map.setLevel(command.level,{animate:false});map.setCenter(point);}
            report('이동 완료');return;
        }
        if(command.level===map.getLevel() && Math.hypot(projected.x-viewport.clientWidth/2,projected.y-viewport.clientHeight/2)<=FIT.negligiblePixel) {
            report('현재 화면 유지');return;
        }
        moving=true;
        if(command.movement==='PAN') {report('배율 유지 · 부드럽게 이동');map.panTo(point);}
        else if(typeof map.jump==='function') {report('중심·배율 함께 이동');map.jump(point,command.level,{animate:{duration:command.duration}});}
        else {report('호환 모드 · 배율 조절 후 이동');map.setLevel(command.level,{anchor:point,animate:false});map.panTo(point);}
    }
    window.cameraMotionDemo={
        connect:function(){if(ready && window.cameraDemoBridge)window.cameraDemoBridge.onReady();},
        measure:function(lat,lng,radius,reduce){return JSON.stringify(measure(lat,lng,radius,reduce));},
        apply:function(command){
            if(!ready)return;
            draw(command.lat,command.lng,command.radius);
            // Keep the newest goal only; wait for SDK idle rather than stacking timed pan commands.
            if(command.movement==='IMMEDIATE' || command.movement.indexOf('BASELINE')===0){pending=null;moving=false;start(command);}
            else if(moving){pending=command;report('이동 중 · 마지막 선택만 대기');}
            else start(command);
        },
        inspect:function(){return JSON.stringify({ready:ready,moving:moving,pending:!!pending,
            lat:map.getCenter().getLat(),lng:map.getCenter().getLng(),level:map.getLevel(),radius:circle.getRadius()});},
        cancel:cancel
    };
    function initialize() {
        map=new kakao.maps.Map(document.getElementById('camera-map'),{center:coordinates(INITIAL.lat,INITIAL.lng),level:INITIAL.level,disableDoubleClickZoom:true});
        circle=new kakao.maps.Circle({center:coordinates(INITIAL.lat,INITIAL.lng),radius:INITIAL.radius,strokeWeight:2,
            strokeColor:'#dc4f78',strokeOpacity:.8,fillColor:'#f3b3c7',fillOpacity:.12});circle.setMap(map);
        marker=new kakao.maps.Marker({position:coordinates(INITIAL.lat,INITIAL.lng)});marker.setMap(map);
        kakao.maps.event.addListener(map,'click',function(event){if(window.cameraDemoBridge)window.cameraDemoBridge.onPicked(event.latLng.getLat(),event.latLng.getLng());});
        kakao.maps.event.addListener(circle,'click',function(event){if(window.cameraDemoBridge)window.cameraDemoBridge.onPicked(event.latLng.getLat(),event.latLng.getLng());});
        kakao.maps.event.addListener(map,'idle',function(){
            moving=false;
            if(pending){const next=pending;pending=null;start(next);}else report('이동 완료');
        });
        kakao.maps.event.addListener(map,'dragstart',function(){cancel();if(window.cameraDemoBridge)window.cameraDemoBridge.onUserInteraction();});
        document.getElementById('camera-map').addEventListener('wheel',function(){cancel();if(window.cameraDemoBridge)window.cameraDemoBridge.onUserInteraction();},{capture:true,passive:true});
        window.addEventListener('resize',function(){cancel();const center=map.getCenter();map.relayout();map.setCenter(center);});
        notice.hidden=true;ready=true;window.cameraMotionDemo.connect();
    }
    if(!window.KAKAO_MAP_KEY){notice.textContent='지도 키를 불러오지 못했어요. 기존 팀 키 설정을 확인해 주세요.';return;}
    const script=document.createElement('script');script.src='https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey='+encodeURIComponent(window.KAKAO_MAP_KEY);
    script.onload=function(){if(window.kakao && kakao.maps)kakao.maps.load(initialize);else notice.textContent='지도 SDK를 불러오지 못했어요.';};
    script.onerror=function(){notice.textContent='지도 연결에 실패했어요. 네트워크와 키의 허용 도메인을 확인해 주세요.';};
    document.head.appendChild(script);
}());
