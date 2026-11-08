(function () {
    'use strict';
    // Owns visit selection; map operations are supplied by the existing map adapter.
    window.createItineraryRail = function (options) {
        const surface = document.getElementById('map-surface');
        const rail = document.getElementById('itinerary-rail');
        const viewport = document.getElementById('itinerary-viewport');
        const list = document.getElementById('itinerary-list');
        const detail = document.getElementById('place-detail');
        const closeButton = document.getElementById('detail-close');
        const counter = document.getElementById('itinerary-count');
        const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
        let visits = [], cards = [], activePlaceId = null, expandedPlaceId = null;
        let scrollTimer, closeTimer, animationFrame, animationVersion = 0, programmatic = false;
        let signature = '';
        let linkVersion = 0;
        let panelManaged = false, activeTab = 'course', collections = { course: [], nearby: [] };
        const courseTab = document.getElementById('course-tab'), nearbyTab = document.getElementById('nearby-tab');
        function switchTab(tab, user) {
            activeTab = tab;
            courseTab.setAttribute('aria-selected', String(tab === 'course'));
            nearbyTab.setAttribute('aria-selected', String(tab === 'nearby'));
            courseTab.tabIndex = tab === 'course' ? 0 : -1;
            nearbyTab.tabIndex = tab === 'nearby' ? 0 : -1;
            document.getElementById('itinerary-title').textContent = tab === 'course' ? '추천 코스' : '주변 장소';
            document.getElementById('itinerary-hint').textContent = tab === 'course'
                ? '방문 순서대로 탐색 · 선택된 카드를 눌러 상세' : '가까운 순으로 탐색 · 선택된 카드를 눌러 상세';
            if (options.onTabChange) options.onTabChange(tab, collections[tab], user);
        }
        courseTab.addEventListener('click', function () { switchTab('course', true); });
        nearbyTab.addEventListener('click', function () { switchTab('nearby', true); });
        [courseTab, nearbyTab].forEach(function (tab) {
            tab.addEventListener('keydown', function (event) {
                if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                    event.preventDefault(); event.stopPropagation();
                    switchTab(activeTab === 'course' ? 'nearby' : 'course', true);
                    (activeTab === 'course' ? courseTab : nearbyTab).focus();
                }
            });
        });

        function cancelScroll() {
            animationVersion++;
            cancelAnimationFrame(animationFrame);
            clearTimeout(scrollTimer);
            programmatic = false;
            viewport.classList.remove('is-centering');
        }
        function closeDetail(returnFocus) {
            linkVersion++;
            expandedPlaceId = null;
            surface.classList.remove('has-detail');
            detail.classList.remove('is-open');
            detail.setAttribute('aria-hidden', 'true');
            cards.forEach(function (card) { card.setAttribute('aria-expanded', 'false'); });
            clearTimeout(closeTimer);
            // Disable tab navigation immediately, while allowing the exit transition.
            detail.querySelectorAll('button,a').forEach(function (node) { node.tabIndex = -1; });
            closeTimer = setTimeout(function () { detail.hidden = true; }, reduced.matches ? 0 : 200);
            if (returnFocus) {
                const index = visits.findIndex(function (v) { return v.visitId === activePlaceId; });
                if (cards[index]) cards[index].focus({ preventScroll: true });
            }
        }
        function activate(id) {
            const index = visits.findIndex(function (v) { return v.visitId === id; });
            if (index < 0) return;
            if (id !== activePlaceId) closeDetail(false);
            activePlaceId = id;
            cards.forEach(function (card, i) {
                card.setAttribute('aria-selected', String(i === index));
                card.tabIndex = i === index ? 0 : -1;
            });
            counter.textContent = String(index + 1).padStart(2, '0') + ' / ' + String(visits.length).padStart(2, '0');
            options.onActive(id);
        }
        function targetScroll(index) {
            const item = list.children[index];
            return item.offsetTop + item.offsetHeight / 2 - viewport.clientHeight / 2;
        }
        function center(id, instant) {
            cancelScroll();
            const index = visits.findIndex(function (v) { return v.visitId === id; });
            if (index < 0 || !viewport.clientHeight) return;
            const target = targetScroll(index);
            const from = viewport.scrollTop;
            const version = animationVersion;
            programmatic = true;
            viewport.classList.add('is-centering');
            const started = performance.now();
            function frame(now) {
                if (version !== animationVersion) return;
                const t = instant || reduced.matches ? 1 : Math.min(1, (now - started) / 200);
                viewport.scrollTop = from + (target - from) * (1 - Math.pow(1 - t, 3));
                if (t < 1) animationFrame = requestAnimationFrame(frame);
                else animationFrame = requestAnimationFrame(function () {
                    if (version !== animationVersion) return;
                    viewport.classList.remove('is-centering');
                    programmatic = false;
                });
            }
            animationFrame = requestAnimationFrame(frame);
        }
        function layout() {
            if (rail.hidden) return;
            const padding = Math.max(0, (viewport.clientHeight - 78) / 2);
            list.style.paddingTop = padding + 'px';
            list.style.paddingBottom = padding + 'px';
            center(activePlaceId, true);
        }
        function fact(label, value) {
            const dt = document.createElement('dt'), dd = document.createElement('dd');
            dt.textContent = label; dd.textContent = value;
            document.getElementById('detail-facts').appendChild(dt);
            document.getElementById('detail-facts').appendChild(dd);
        }
        function openDetail(id) {
            const index = visits.findIndex(function (v) { return v.visitId === id; });
            if (index < 0) return;
            activate(id);
            const place = visits[index];
            expandedPlaceId = id;
            clearTimeout(closeTimer);
            document.getElementById('detail-title').textContent = place.name;
            document.getElementById('detail-category').textContent = (activeTab === 'nearby' ? '주변 장소 · ' : (index + 1) + '번째 방문 · ') + category(place.category);
            const facts = document.getElementById('detail-facts');
            facts.textContent = '';
            if (Number.isFinite(place.price) && place.price >= 0) fact('1인 예상 가격', place.price.toLocaleString('ko-KR') + '원');
            if (Number.isFinite(place.rating) && place.rating > 0) fact('평점', place.rating.toFixed(1));
            const extra = document.getElementById('detail-extra');
            extra.textContent = '';
            [['주소', place.address], ['설명', place.description], ['영업시간', place.openingHours], ['예상 방문 시간', place.visitDuration]].forEach(function (pair) {
                if (!pair[1]) return;
                const p = document.createElement('p'); p.textContent = pair[0] + ' · ' + pair[1]; extra.appendChild(p);
            });
            const link = document.getElementById('detail-map-link');
            const version = ++linkVersion;
            link.href = 'https://map.kakao.com/link/search/' + encodeURIComponent('부산 ' + place.name);
            link.textContent = '카카오맵에서 가게 검색 ↗';
            link.setAttribute('aria-busy', 'true');
            if (options.resolvePlaceUrl) options.resolvePlaceUrl(place).then(function (url) {
                if (version !== linkVersion || expandedPlaceId !== id) return;
                link.setAttribute('aria-busy', 'false');
                if (url) { link.href = url; link.textContent = '카카오맵에서 가게 정보 보기 ↗'; }
            });
            detail.hidden = false;
            detail.setAttribute('aria-hidden', 'false');
            detail.querySelectorAll('button,a').forEach(function (node) { node.tabIndex = 0; });
            surface.classList.add('has-detail');
            cards[index].setAttribute('aria-expanded', 'true');
            // Force the initial transition state before entering; no delayed map callbacks.
            detail.getBoundingClientRect();
            detail.classList.add('is-open');
            options.onOpen(place, detail.getBoundingClientRect(), rail.getBoundingClientRect());
        }
        function category(code) {
            return ({ MEAL: '식사', CAFE: '카페', CULTURE: '문화' })[code] || code || '';
        }
        function select(id, expand) {
            activate(id); center(id, false);
            if (expand) openDetail(id);
        }
        viewport.addEventListener('scroll', function () {
            if (programmatic || !visits.length) return;
            clearTimeout(scrollTimer);
            scrollTimer = setTimeout(function () {
                let nearest = 0, distance = Infinity;
                visits.forEach(function (visit, i) {
                    const d = Math.abs(targetScroll(i) - viewport.scrollTop);
                    if (d < distance) { nearest = i; distance = d; }
                });
                activate(visits[nearest].visitId);
                center(activePlaceId, false);
            }, 140);
        });
        ['wheel', 'touchstart', 'pointerdown'].forEach(function (name) {
            viewport.addEventListener(name, function (event) { cancelScroll(); event.stopPropagation(); }, { passive: true });
        });
        ['click', 'dblclick', 'mousedown', 'touchstart', 'touchmove', 'wheel'].forEach(function (name) {
            [rail, detail].forEach(function (node) {
                node.addEventListener(name, function (event) { event.stopPropagation(); }, { passive: true });
            });
        });
        rail.addEventListener('keydown', function (event) {
            if (event.target.getAttribute('role') === 'tab') return;
            const index = visits.findIndex(function (v) { return v.visitId === activePlaceId; });
            let next = index;
            if (event.key === 'ArrowDown') next = Math.min(visits.length - 1, index + 1);
            else if (event.key === 'ArrowUp') next = Math.max(0, index - 1);
            else if (event.key === 'Home') next = 0;
            else if (event.key === 'End') next = visits.length - 1;
            else if (event.key === 'Escape') { event.preventDefault(); closeDetail(true); return; }
            else return;
            event.preventDefault();
            if (visits[next]) { select(visits[next].visitId, false); cards[next].focus({ preventScroll: true }); }
        });
        detail.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') { event.preventDefault(); closeDetail(true); }
        });
        closeButton.addEventListener('click', function () { closeDetail(true); });
        if (window.ResizeObserver) new ResizeObserver(layout).observe(viewport);
        window.addEventListener('resize', function () {
            layout();
            if (expandedPlaceId) openDetail(expandedPlaceId);
        });
        return {
            setCollections: function (course, nearby) {
                const changed = JSON.stringify(collections.course) !== JSON.stringify(course);
                collections = { course: course, nearby: nearby };
                panelManaged = true;
                switchTab(course.length && changed ? 'course' : !course.length ? 'nearby' : activeTab, false);
            },
            setItems: function (items) {
                const nextSignature = JSON.stringify([activeTab, items]);
                if (signature === nextSignature) return visits;
                signature = nextSignature;
                cancelScroll(); closeDetail(false);
                activePlaceId = null; expandedPlaceId = null;
                const occurrences = new Map();
                visits = items.map(function (place) {
                    const id = String(place.placeId || place.recordId || (place.name + ':' + place.lat + ':' + place.lng));
                    const count = (occurrences.get(id) || 0) + 1; occurrences.set(id, count);
                    return Object.assign({}, place, { visitId: id + ':' + count });
                });
                list.textContent = ''; cards = [];
                rail.hidden = panelManaged ? false : !visits.length;
                document.getElementById('itinerary-empty').hidden = !!visits.length;
                document.getElementById('itinerary-empty').textContent = activeTab === 'course'
                    ? '아직 추천 코스가 없어요. 왼쪽에서 조건을 설정한 뒤 코스를 추천받아 주세요.'
                    : '출발 위치를 선택하면 주변 장소를 보여드려요. 결과가 없으면 반경이나 활동을 바꿔 보세요.';
                viewport.hidden = !visits.length;
                counter.textContent = visits.length ? '' : '0곳';
                visits.forEach(function (place, index) {
                    const li = document.createElement('li'), button = document.createElement('button');
                    li.setAttribute('role', 'presentation');
                    button.type = 'button'; button.className = 'mini-place-card';
                    button.setAttribute('role', 'option'); button.setAttribute('aria-controls', 'place-detail');
                    button.setAttribute('aria-expanded', 'false'); button.dataset.visitId = place.visitId;
                    button.setAttribute('aria-label', (activeTab === 'nearby' ? '주변 장소, ' : (index + 1) + '번째 방문, ') + place.name + ', ' + category(place.category));
                    const number = document.createElement('span'); number.className = 'visit-number'; number.textContent = String(index + 1).padStart(2, '0');
                    const copy = document.createElement('span'); copy.className = 'visit-copy';
                    const name = document.createElement('span'); name.className = 'visit-name'; name.textContent = place.name;
                    const meta = document.createElement('span'); meta.className = 'visit-meta'; meta.textContent = category(place.category)
                        + (activeTab === 'nearby' && Number.isFinite(place.price) ? ' · ' + place.price.toLocaleString('ko-KR') + '원/인' : '');
                    const current = document.createElement('span'); current.className = 'visit-current'; current.textContent = '선택됨';
                    meta.appendChild(current); copy.appendChild(name); copy.appendChild(meta); button.appendChild(number); button.appendChild(copy);
                    button.addEventListener('click', function () {
                        if (activePlaceId !== place.visitId) select(place.visitId, false);
                        else if (expandedPlaceId === place.visitId) closeDetail(false);
                        else openDetail(place.visitId);
                    });
                    li.appendChild(button); list.appendChild(li); cards.push(button);
                });
                if (visits.length) { activate(visits[0].visitId); layout(); }
                return visits;
            },
            openVisit: function (id) { select(id, true); },
            openPlace: function (recordId) {
                const visit = visits.find(function (v) { return String(v.recordId) === String(recordId); });
                if (visit) select(visit.visitId, true);
            },
            getVisits: function () { return visits; },
            getState: function () { return { activePlaceId: activePlaceId, expandedPlaceId: expandedPlaceId, activeTab: activeTab }; }
        };
    };
}());
