(function () {
    'use strict';
    // Shared by both maps. Accumulate wheel input before one SDK zoom level.
    window.installInstantZoom = function (element, map, options) {
        // Transform only the SDK map; search, details and legend are sibling elements.
        if (options && options.fractional === true) {
            return installFractionalZoom(element, map);
        }
        const motion = options && options.motion || 'auto';
        const interval = 320; // Let the SDK's 300ms animation finish before the next level.
        const threshold = 40; // One normal wheel notch reacts immediately; tiny trackpad noise accumulates.
        const duration = 300;
        let pending = null, timer, resetTimer, delta = 0, lastApplied = -Infinity;
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
        function apply(request) {
            const current = map.getLevel();
            const level = Math.max(1, Math.min(14, current + request.direction));
            if (level === current) return;
            const rect = element.getBoundingClientRect();
            const point = new kakao.maps.Point(
                Math.max(0, Math.min(element.clientWidth, (request.x - rect.left) * element.clientWidth / rect.width)),
                Math.max(0, Math.min(element.clientHeight, (request.y - rect.top) * element.clientHeight / rect.height)));
            const anchor = map.getProjection().coordsFromContainerPoint(point);
            const animate = motion === 'off' || (motion !== 'on' && reducedMotion.matches) ? false : {duration: duration};
            map.setLevel(level, { anchor: anchor, animate: animate });
            lastApplied = performance.now();
        }
        function flush() {
            timer = null;
            const request = pending; pending = null;
            if (request) apply(request);
        }
        function wheel(event) {
            if (!event.deltaY) return;
            event.preventDefault(); event.stopImmediatePropagation();
            const pixels = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? element.clientHeight : 1);
            if (pending && pending.direction !== Math.sign(pixels)) {
                clearTimeout(timer); timer = null; pending = null;
            }
            if (delta && Math.sign(delta) !== Math.sign(pixels)) delta = 0;
            delta += pixels;
            clearTimeout(resetTimer);
            resetTimer = setTimeout(function () { delta = 0; }, 300);
            if (Math.abs(delta) < threshold) return;
            pending = { direction: delta > 0 ? 1 : -1, x: event.clientX, y: event.clientY };
            delta = 0;
            if (timer) return;
            const wait = interval - (performance.now() - lastApplied);
            if (wait <= 0) flush(); else timer = setTimeout(flush, wait);
        }
        function doubleClick(event) {
            event.preventDefault(); event.stopImmediatePropagation();
            clearTimeout(timer); clearTimeout(resetTimer); timer = null; pending = null; delta = 0;
            // Double-click is an explicit one-level command; rapid repeats are ignored.
            if (performance.now() - lastApplied >= interval)
                apply({ direction: event.shiftKey ? 1 : -1, x: event.clientX, y: event.clientY });
        }
        element.addEventListener('wheel', wheel, { capture: true, passive: false });
        element.addEventListener('dblclick', doubleClick, { capture: true });
        return function dispose() {
            clearTimeout(timer); clearTimeout(resetTimer); pending = null;
            element.removeEventListener('wheel', wheel, true);
            element.removeEventListener('dblclick', doubleClick, true);
        };
    };

    function installFractionalZoom(element, map) {
        let virtualLevel = map.getLevel();
        let sdkLevel = virtualLevel;
        const savedStyle = {width: element.style.width, height: element.style.height,
            transform: element.style.transform, transformOrigin: element.style.transformOrigin};
        function setScale(factor) {
            element.style.width = (100 / factor) + '%';
            element.style.height = (100 / factor) + '%';
            element.style.transformOrigin = '0 0';
            element.style.transform = 'scale(' + factor + ')';
            element.dataset.zoomScale = String(factor);
        }
        function reset() {
            setScale(1);
            map.relayout();
            virtualLevel = sdkLevel = map.getLevel();
        }
        window.resetMapZoom = reset;
        function zoom(amount, event) {
            if (map.getLevel() !== sdkLevel) reset();
            const rect = element.getBoundingClientRect();
            const x = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
            const y = Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height));
            const projection = map.getProjection();
            const anchor = projection.coordsFromContainerPoint(new kakao.maps.Point(x * element.clientWidth, y * element.clientHeight));
            virtualLevel = Math.round(Math.max(1, Math.min(14, virtualLevel + amount)) * 1e9) / 1e9;
            const nextLevel = Math.ceil(virtualLevel - 1e-9);
            const factor = Math.max(1, Math.min(2, Math.pow(2, nextLevel - virtualLevel)));
            setScale(factor);
            map.relayout();
            if (nextLevel !== sdkLevel) map.setLevel(nextLevel, {animate: false});
            sdkLevel = nextLevel;
            const updated = map.getProjection();
            const actual = updated.containerPointFromCoords(anchor);
            map.setCenter(updated.coordsFromContainerPoint(new kakao.maps.Point(
                element.clientWidth / 2 + actual.x - x * element.clientWidth,
                element.clientHeight / 2 + actual.y - y * element.clientHeight)));
        }
        function wheel(event) {
            if (!event.deltaY) return;
            event.preventDefault(); event.stopImmediatePropagation();
            const pixels = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? element.clientHeight : 1);
            // A 120px mouse notch changes scale by about 7%; small inputs react immediately.
            zoom(Math.max(-120, Math.min(120, pixels)) / 1200, event);
        }
        function doubleClick(event) {
            event.preventDefault(); event.stopImmediatePropagation();
            zoom(event.shiftKey ? 0.25 : -0.25, event);
        }
        element.addEventListener('wheel', wheel, {capture: true, passive: false});
        element.addEventListener('dblclick', doubleClick, {capture: true});
        return function () {
            reset();
            element.removeEventListener('wheel', wheel, true);
            element.removeEventListener('dblclick', doubleClick, true);
            delete window.resetMapZoom;
            Object.keys(savedStyle).forEach(function (key) { element.style[key] = savedStyle[key]; });
            delete element.dataset.zoomScale;
            map.relayout();
        };
    }
}());
