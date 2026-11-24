(function () {
    'use strict';
    /** SDK 경계는 화면 밖으로 밀고 입력·카메라는 보이는 영역을 기준으로 계산한다. */
    window.createBufferedMapViewport = function (viewport, canvas) {
        let map = null, buffered = true, padX = 0, padY = 0;
        const footerMargins = new WeakMap();
        function positionFooters() {
            // 원래 SDK 로고·출처 노드를 유지하며 화면 안쪽으로만 옮긴다.
            Array.from(canvas.children).forEach(function (node) {
                if (node.style.position !== 'absolute' || !node.style.bottom || node.style.bottom === 'auto'
                    || node.style.top && node.style.top !== 'auto') return;
                let original = footerMargins.get(node);
                if (!original) {
                    original = {bottom: parseFloat(node.style.marginBottom) || 0,
                        left: parseFloat(node.style.marginLeft) || 0, right: parseFloat(node.style.marginRight) || 0};
                    footerMargins.set(node, original);
                }
                node.style.marginBottom = original.bottom + padY + 'px';
                if (node.style.left && node.style.left !== 'auto') node.style.marginLeft = original.left + padX + 'px';
                if (node.style.right && node.style.right !== 'auto') node.style.marginRight = original.right + padX + 'px';
            });
        }
        function resize() {
            padX = buffered ? Math.min(256, Math.round(viewport.clientWidth * .25)) : 0;
            padY = buffered ? Math.min(256, Math.round(viewport.clientHeight * .25)) : 0;
            canvas.style.left = -padX + 'px'; canvas.style.top = -padY + 'px';
            canvas.style.width = viewport.clientWidth + padX * 2 + 'px';
            canvas.style.height = viewport.clientHeight + padY * 2 + 'px';
            positionFooters();
        }
        function projection() {
            const raw = map.getProjection();
            return {
                containerPointFromCoords: function (coords) {
                    const point = raw.containerPointFromCoords(coords);
                    return new kakao.maps.Point(point.x - padX, point.y - padY);
                },
                coordsFromContainerPoint: function (point) {
                    return raw.coordsFromContainerPoint(new kakao.maps.Point(point.x + padX, point.y + padY));
                }
            };
        }
        const observer = new MutationObserver(positionFooters);
        observer.observe(canvas, {childList: true});
        resize();
        return {resize: resize, projection: projection,
            attach: function (sdkMap) { map = sdkMap; positionFooters(); kakao.maps.event.addListener(map, 'bounds_changed', positionFooters); },
            setBuffered: function (enabled) { buffered = !!enabled; resize(); },
            inspect: function () { return {padX: padX, padY: padY, width: viewport.clientWidth, height: viewport.clientHeight,
                renderWidth: canvas.clientWidth, renderHeight: canvas.clientHeight}; }
        };
    };
}());
