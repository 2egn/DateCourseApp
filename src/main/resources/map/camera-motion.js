(function () {
    'use strict';
    // SDK adapter only: movement decisions belong to CameraMotionPolicy in Java.
    window.createMapCamera = function (map, element, decide) {
        const padding = 48;
        let moving = false, pending = null, last = null;
        function cancel() { pending = null; moving = false; }
        function measure(goal) {
            const projection = map.getProjection(), width = element.clientWidth, height = element.clientHeight;
            const halfW = Math.max(1, width / 2 - padding), halfH = Math.max(1, height / 2 - padding);
            let center = goal.center || map.getCenter(), fitLevel = map.getLevel(), overflow = 0;
            if (goal.points && goal.points.length) {
                let minLat = Infinity, maxLat = -Infinity, minLng = Infinity, maxLng = -Infinity;
                goal.points.forEach(function (p) {
                    minLat = Math.min(minLat, p.getLat()); maxLat = Math.max(maxLat, p.getLat());
                    minLng = Math.min(minLng, p.getLng()); maxLng = Math.max(maxLng, p.getLng());
                });
                if (!goal.keepCenter) center = new kakao.maps.LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
                const target = projection.containerPointFromCoords(center);
                let ratio = 0;
                goal.points.forEach(function (p) {
                    const point = projection.containerPointFromCoords(p);
                    ratio = Math.max(ratio, Math.abs(point.x - target.x) / halfW, Math.abs(point.y - target.y) / halfH);
                    overflow = Math.max(overflow, Math.abs(point.x - width / 2) / halfW, Math.abs(point.y - height / 2) / halfH);
                });
                // A single stop keeps its current zoom instead of zooming to SDK maximum.
                if (ratio > 0) fitLevel = Math.max(1, Math.min(14, map.getLevel() + Math.ceil(Math.log(ratio) / Math.LN2)));
            }
            const point = projection.containerPointFromCoords(center);
            return {center:center, frame:{level:map.getLevel(), fitLevel:goal.level || fitLevel,
                screenDistance:Math.hypot(point.x - width / 2, point.y - height / 2) / Math.max(1, Math.min(width, height)),
                overflowRatio:overflow, reducedMotion:window.matchMedia('(prefers-reduced-motion: reduce)').matches}};
        }
        function execute(goal) {
            const measured = measure(goal), action = decide(measured.frame, goal.intent);
            last = {intent:goal.intent, movement:action.movement, level:action.level};
            if (action.movement === 'NONE') return;
            const center = measured.center;
            if (action.movement === 'IMMEDIATE') {
                if (typeof map.jump === 'function') map.jump(center, action.level, {animate:false});
                else {map.setLevel(action.level, {animate:false}); map.setCenter(center);}
                return;
            }
            moving = true;
            if (action.movement === 'PAN') map.panTo(center);
            else if (typeof map.jump === 'function') map.jump(center, action.level, {animate:{duration:action.duration}});
            else {map.setLevel(action.level, {anchor:center, animate:false}); map.panTo(center);}
        }
        function request(goal) {
            if (window.disposeMapZoom && window.disposeMapZoom.cancel) window.disposeMapZoom.cancel();
            // Retain the goal, not a precomputed transition: remeasure after SDK idle.
            if (goal.intent === 'RESET') {cancel(); execute(goal);}
            else if (moving) pending = goal;
            else execute(goal);
        }
        function onIdle() {
            moving = false;
            if (pending) {const goal = pending; pending = null; execute(goal);}
        }
        kakao.maps.event.addListener(map, 'idle', onIdle);
        kakao.maps.event.addListener(map, 'dragstart', cancel);
        function manual(event) {
            if (event.target && event.target.closest && event.target.closest('.walking-route-tooltip')) return;
            cancel();
        }
        // Install before wheel zoom handlers, which stop immediate propagation.
        element.addEventListener('wheel', manual, {capture:true, passive:true});
        element.addEventListener('dblclick', manual, {capture:true});
        return {request:request, cancel:cancel,
            inspect:function () {return {moving:moving, pending:!!pending, last:last, level:map.getLevel(),
                lat:map.getCenter().getLat(), lng:map.getCenter().getLng()};},
            dispose:function () {
                cancel(); kakao.maps.event.removeListener(map, 'idle', onIdle);
                kakao.maps.event.removeListener(map, 'dragstart', cancel);
                element.removeEventListener('wheel', manual, true); element.removeEventListener('dblclick', manual, true);
            }};
    };
}());
