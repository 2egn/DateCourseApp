(function () {
    'use strict';
    // Capture desktop inputs before the SDK. Native touch pinch/drag stays available.
    window.installInstantZoom = function (element, map, options) {
        const motion = options && options.motion || 'auto';
        const interval = 120;
        let pending = null, timer, resetTimer, delta = 0, lastApplied = -Infinity;
        let zoomAnimation;
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

        function softenZoom(direction, point) {
            if (zoomAnimation) { zoomAnimation.cancel(); zoomAnimation = null; }
            if (motion === 'off' || (motion !== 'on' && reducedMotion.matches) || !element.animate) return;
            // The actual camera commits instantly; a small settle effect avoids the SDK's hidden-marker animation.
            element.style.transformOrigin = point.x + 'px ' + point.y + 'px';
            const animation = element.animate([
                { transform: 'scale(' + (direction < 0 ? 1.025 : 1.012) + ')', opacity: 0.88 },
                { transform: 'scale(1)', opacity: 1 }
            ], { duration: 160, easing: 'cubic-bezier(.2,.7,.3,1)' });
            zoomAnimation = animation;
            animation.onfinish = function () {
                if (zoomAnimation !== animation) return;
                zoomAnimation = null;
                element.style.removeProperty('transform-origin');
            };
        }

        function apply(request) {
            const current = map.getLevel();
            const level = Math.max(1, Math.min(14, current + request.direction));
            if (level === current) return;
            const rect = element.getBoundingClientRect();
            const point = new kakao.maps.Point(
                Math.max(0, Math.min(element.clientWidth, (request.x - rect.left) * element.clientWidth / rect.width)),
                Math.max(0, Math.min(element.clientHeight, (request.y - rect.top) * element.clientHeight / rect.height)));
            const anchor = map.getProjection().coordsFromContainerPoint(point);
            map.setLevel(level, { anchor: anchor, animate: false });
            softenZoom(request.direction, point);
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
            resetTimer = setTimeout(function () { delta = 0; }, 160);
            if (Math.abs(delta) < 30) return;
            pending = { direction: delta > 0 ? 1 : -1, x: event.clientX, y: event.clientY };
            delta = 0;
            if (timer) return;
            const wait = interval - (performance.now() - lastApplied);
            if (wait <= 0) flush(); else timer = setTimeout(flush, wait);
        }
        function doubleClick(event) {
            event.preventDefault(); event.stopImmediatePropagation();
            clearTimeout(timer); clearTimeout(resetTimer); timer = null; pending = null; delta = 0;
            apply({ direction: event.shiftKey ? 1 : -1, x: event.clientX, y: event.clientY });
        }
        element.addEventListener('wheel', wheel, { capture: true, passive: false });
        element.addEventListener('dblclick', doubleClick, { capture: true });
        return function dispose() {
            clearTimeout(timer); clearTimeout(resetTimer); pending = null;
            if (zoomAnimation) zoomAnimation.cancel();
            element.style.removeProperty('transform-origin');
            element.removeEventListener('wheel', wheel, true);
            element.removeEventListener('dblclick', doubleClick, true);
        };
    };
}());
