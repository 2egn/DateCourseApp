(function () {
    "use strict";
    let camera, rangeAdjusting = false;
    function decideCamera(frame, intent) {
        const bridge = window.javaMapBridge;
        if (bridge && typeof bridge.decideCameraTransition === 'function')
            return JSON.parse(bridge.decideCameraTransition(intent, frame.level, frame.fitLevel,
                frame.screenDistance, frame.overflowRatio, frame.reducedMotion));
        // Standalone browser without Java: retain a usable map, without duplicating policy.
        return {movement:'IMMEDIATE', level:intent === 'FOCUS' ? frame.level : frame.fitLevel, duration:0};
    }
    function circlePoints(lat, lng, radius) {
        const dy = radius / 6378137 * 180 / Math.PI, dx = dy / Math.cos(lat * Math.PI / 180);
        return [new kakao.maps.LatLng(lat - dy, lng - dx), new kakao.maps.LatLng(lat + dy, lng + dx)];
    }
    window.setCameraRangeAdjusting = function (value) {rangeAdjusting = !!value;};
    window.inspectMapCamera = function () {return camera && camera.inspect();};

    // Set false to restore the original SDK wheel/double-click zoom behavior.
    const USE_INSTANT_ZOOM = true;
    // Experimental map-only scaling inside WebView; false restores SDK-level wheel behavior.
    const USE_FRACTIONAL_ZOOM = false;

    const PIN_INPUT = Object.freeze({width:56, height:68, anchorX:28, anchorY:60,
        exitMargin:6, tieTolerance:6, movementTolerance:6, leaveDelay:150});

    let map;
    let marker;
    let circle;
    let circleOutline;
    let nextCircle;
    let nextCircleOutline;
    let nextRadiusMeter = 500;
    let displayedNextRange = null;
    let selectedPosition;
    let selectedPlaceName = "";
    let originLocked = false;
    window.setOriginLocked = function (value) { originLocked = !!value; };
    let locationVersion = 0;
    let locationNameTimer;
    let radiusMeter = 500;
    let ready = false;
    let zoomInstalled = false;
    let routePresentation = null;
    let routeCandidate = null;
    let routeCandidates = [];
    let routeLocked = false;
    let suppressLocationNotification = false;
    let failureMessage = "";
    const status = document.getElementById("map-status");
    const searchForm = document.getElementById("search-form");
    const searchInput = document.getElementById("place-query");
    const searchButton = document.getElementById("search-submit");
    const searchMessage = document.getElementById("search-message");
    const searchResults = document.getElementById("search-results");
    const searchClose = document.getElementById("search-close");
    const searchPopup = document.getElementById("search-popup");
    let activeSearchIndex = -1;
    let places;
    let geocoder;
    let searchVersion = 0;
    let searchTimer;
    let searchInputTimer;

    function openExternalLink(address) {
        if (window.javaMapBridge && typeof window.javaMapBridge.openExternalLink === "function") {
            window.javaMapBridge.openExternalLink(address);
        } else {
            status.textContent = "외부 링크는 앱에서 기본 브라우저로 열 수 있어요.";
        }
    }

    // Keep Kakao's attribution links intact, but open their full website in a browser.
    document.addEventListener("click", function (event) {
        const target = event.target.nodeType === 1 ? event.target : event.target.parentElement;
        const link = target && target.closest("a[href]");
        if (!link || link.id==='detail-report' || !/^https?:/i.test(link.href)) return;
        event.preventDefault();
        event.stopImmediatePropagation();
        openExternalLink(link.href);
    }, true);
    window.open = function (address) {
        if (address) openExternalLink(new URL(address, window.location.href).href);
        return null;
    };

    function updateSearchPopup() {
        const hasResults = searchResults.childElementCount > 0;
        searchPopup.hidden = !hasResults && !searchMessage.textContent;
        searchInput.setAttribute('aria-expanded', String(!searchPopup.hidden));
    }
    function searchNotice(message) {
        searchMessage.textContent = message;
        updateSearchPopup();
    }
    function activateSearchResult(index) {
        const buttons = searchResults.querySelectorAll('button');
        activeSearchIndex = index;
        buttons.forEach(function (button, i) { button.setAttribute('aria-selected', String(i === index)); });
        const button = buttons[index];
        if (!button) { searchInput.removeAttribute('aria-activedescendant'); return; }
        searchInput.setAttribute('aria-activedescendant', button.id);
        button.focus({ preventScroll: true });
        if (button.offsetTop < searchResults.scrollTop) searchResults.scrollTop = button.offsetTop;
        else if (button.offsetTop + button.offsetHeight > searchResults.scrollTop + searchResults.clientHeight)
            searchResults.scrollTop = button.offsetTop + button.offsetHeight - searchResults.clientHeight;
    }

    function clearSearchResults() {
        while (searchResults.firstChild) searchResults.removeChild(searchResults.firstChild);
        searchClose.hidden = true;
        activeSearchIndex = -1;
        searchInput.removeAttribute('aria-activedescendant');
        updateSearchPopup();
    }

    function dismissSearch() {
        searchVersion++;
        clearTimeout(searchTimer);
        clearTimeout(searchInputTimer);
        clearSearchResults();
        searchNotice("");
    }

    function renderSearchResults(results) {
        searchClose.hidden = false;
        results.forEach(function (place, index) {
            const item = document.createElement("li");
            item.setAttribute('role', 'presentation');
            const button = document.createElement("button");
            button.type = "button";
            button.id = 'search-option-' + searchVersion + '-' + index;
            button.tabIndex = -1;
            button.setAttribute('role', 'option');
            button.setAttribute('aria-selected', 'false');
            button.addEventListener('mousedown', function (event) { event.preventDefault(); });
            const name = document.createElement("span");
            name.className = "place-name";
            name.textContent = place.place_name;
            const address = document.createElement("span");
            address.className = "place-address";
            address.textContent = place.road_address_name || place.address_name;
            button.appendChild(name);
            button.appendChild(address);
            button.addEventListener("click", function () {
                const latitude = Number(place.y);
                const longitude = Number(place.x);
                if (window.selectMapLocation(latitude, longitude, place.place_name) === false) return;

                searchInput.value = place.place_name;
                clearSearchResults();
                searchNotice("");
                searchInput.focus();
            });
            item.appendChild(button);
            searchResults.appendChild(item);
        });
        updateSearchPopup();
    }

    function searchPlaces() {
        const version = ++searchVersion;
        clearTimeout(searchTimer);
        clearTimeout(searchInputTimer);
        clearSearchResults();
        const query = searchInput.value.trim();
        if (!query) {
            searchNotice("");
            return;
        }
        if (!ready || !places) {
            searchNotice("장소 검색이 준비될 때까지 기다려 주세요.");
            return;
        }
        searchNotice("부산 지역에서 검색하고 있어요…");
        searchTimer = setTimeout(function () {
            if (version !== searchVersion) return;
            searchVersion++;
            searchNotice("검색 시간이 초과됐어요. 다시 검색해 주세요.");
        }, 15000);
        try {
            const keyword = query.indexOf("부산") === -1 ? "부산 " + query : query;
            places.keywordSearch(keyword, function (data, resultStatus) {
                if (version !== searchVersion) return;
                clearTimeout(searchTimer);
                if (resultStatus === kakao.maps.services.Status.ZERO_RESULT) {
                    searchNotice("검색 결과가 없어요. 다른 장소 이름을 입력해 주세요.");
                    return;
                }
                if (resultStatus !== kakao.maps.services.Status.OK) {
                    searchNotice("검색하지 못했어요. 잠시 후 다시 시도해 주세요.");
                    return;
                }
                const results = data.filter(function (place) {
                    const address = place.address_name || place.road_address_name || "";
                    return /^(부산|부산광역시)\s/.test(address)
                        && place.y && place.x
                        && Number.isFinite(Number(place.y)) && Math.abs(Number(place.y)) <= 90
                        && Number.isFinite(Number(place.x)) && Math.abs(Number(place.x)) <= 180;
                });
                if (!results.length) {
                    searchNotice("부산 지역의 결과가 없어요. 장소 이름을 더 구체적으로 입력해 주세요.");
                    return;
                }
                renderSearchResults(results);
                searchNotice("검색 결과 " + results.length + "개 · 출발할 장소를 선택해 주세요.");
            }, { size: 15 });
        } catch (failure) {
            clearTimeout(searchTimer);
            searchNotice("검색하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        searchPlaces();
    });

    function scheduleSearch(event) {
        dismissSearch();
        if (event.isComposing || !searchInput.value.trim()) return;
        searchInputTimer = setTimeout(searchPlaces, 300);
    }
    searchInput.addEventListener("input", scheduleSearch);
    searchInput.addEventListener("compositionend", scheduleSearch);

    searchClose.addEventListener("click", dismissSearch);
    document.getElementById('place-search').addEventListener("keydown", function (event) {
        const fromResult = searchResults.contains(event.target);
        if (event.target !== searchInput && !fromResult) return;
        const aliases = { Down: 'ArrowDown', Up: 'ArrowUp', Esc: 'Escape' };
        const codes = { 38: 'ArrowUp', 40: 'ArrowDown', 13: 'Enter', 27: 'Escape', 9: 'Tab' };
        const key = aliases[event.key] || (event.key && event.key !== 'Unidentified' ? event.key : codes[event.keyCode || event.which]);
        if (event.isComposing || (event.keyCode === 229 && !['ArrowUp', 'ArrowDown', 'Escape'].includes(key))) return;
        const buttons = searchResults.querySelectorAll('button');
        if ((key === 'ArrowDown' || key === 'ArrowUp') && buttons.length) {
            event.preventDefault(); event.stopPropagation();
            if (fromResult && activeSearchIndex === 0 && key === 'ArrowUp') {
                activateSearchResult(-1); searchInput.focus(); return;
            }
            const next = activeSearchIndex < 0 ? (key === 'ArrowDown' ? 0 : buttons.length - 1)
                : Math.max(0, Math.min(buttons.length - 1, activeSearchIndex + (key === 'ArrowDown' ? 1 : -1)));
            activateSearchResult(next);
        } else if (key === 'Enter' && activeSearchIndex >= 0 && buttons[activeSearchIndex]) {
            event.preventDefault(); event.stopPropagation(); buttons[activeSearchIndex].click();
        } else if (key === 'Escape') {
            event.preventDefault(); event.stopPropagation(); dismissSearch(); searchInput.focus();
        } else if (key === 'Tab') dismissSearch();
    }, true);
    document.addEventListener('mousedown', function (event) {
        if (!document.getElementById('place-search').contains(event.target)) dismissSearch();
    });

    function reportFailure(message) {
        ready = false;
        failureMessage = message;
        status.textContent = message;
        searchButton.disabled = true;
        searchNotice("지도를 불러온 뒤 장소를 검색할 수 있어요.");
        if (window.javaMapBridge) window.javaMapBridge.onMapError(message);
    }

    function notifyLocation() {
        if (suppressLocationNotification) return;
        if (selectedPosition && window.javaMapBridge) {
            window.javaMapBridge.onLocationSelected(selectedPosition.getLat(), selectedPosition.getLng());
        }
    }

    // Page load and asynchronous SDK initialization may finish in either order.
    window.connectJavaMapBridge = function () {
        if (!window.javaMapBridge) return;
        if (failureMessage) window.javaMapBridge.onMapError(failureMessage);
        else if (ready) {
            if (USE_INSTANT_ZOOM && !zoomInstalled) {
                window.disposeMapZoom = window.installInstantZoom(document.getElementById('map'), map,
                    { motion: 'auto', fractional: USE_FRACTIONAL_ZOOM });
                zoomInstalled = true;
            }
            window.javaMapBridge.onMapReady();
            notifyLocation();
        }
    };

    window.setMapRadius = function (value) {
        if (!Number.isFinite(value) || value < 200 || value > 2000) return;
        radiusMeter = value;
        if (circle) circle.setRadius(value);
        if (circleOutline) circleOutline.setRadius(value);
        updateStatus();
    };

    function updateStatus() {
        if (selectedPosition) {
            status.textContent = selectedPlaceName;
        }
    }

    function resolveSelectedAddress(latitude, longitude, version) {
        if (!geocoder) {
            selectedPlaceName = "선택한 위치";
            updateStatus();
            return;
        }
        locationNameTimer = setTimeout(function () {
            if (version !== locationVersion) return;
            selectedPlaceName = "선택한 위치";
            updateStatus();
        }, 10000);
        try {
            geocoder.coord2Address(longitude, latitude, function (results, resultStatus) {
                if (version !== locationVersion) return;
                clearTimeout(locationNameTimer);
                const result = resultStatus === kakao.maps.services.Status.OK && results[0];
                const address = result && (result.road_address || result.address);
                selectedPlaceName = address && address.address_name ? address.address_name : "선택한 위치";
                updateStatus();
            });
        } catch (failure) {
            clearTimeout(locationNameTimer);
            selectedPlaceName = "선택한 위치";
            updateStatus();
        }
    }

    window.selectMapLocation = function (latitude, longitude, placeName) {
        if (originLocked && !suppressLocationNotification) {
            if (window.javaMapBridge && typeof window.javaMapBridge.onOriginChangeBlocked === 'function')
                window.javaMapBridge.onOriginChangeBlocked();
            return false;
        }
        if (!ready || !Number.isFinite(latitude) || !Number.isFinite(longitude)
            || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
        const version = ++locationVersion;
        clearTimeout(locationNameTimer);
        const hasName = typeof placeName === "string" && placeName.trim().length > 0;
        selectedPlaceName = hasName ? placeName.trim() : "주소를 확인하고 있어요…";
        selectedPosition = new kakao.maps.LatLng(latitude, longitude);
        marker.setPosition(selectedPosition);
        marker.setMap(map);
        circle.setPosition(selectedPosition);
        circle.setRadius(radiusMeter);
        circle.setMap(map);
        circleOutline.setPosition(selectedPosition);
        circleOutline.setRadius(radiusMeter);
        circleOutline.setMap(map);
        updateStatus();
        notifyLocation();
        if (!suppressLocationNotification && !routePresentation) camera.request({intent:'FOCUS', center:selectedPosition, points:circlePoints(latitude, longitude, radiusMeter)});
        if (!hasName) resolveSelectedAddress(latitude, longitude, version);
        return true;
    };

    let resultMarkers = [];
    let resultSignature = '';
    let renderedRouteKey = '';
    let plannerCourse = [];
    let renderingResults = false, currentPanelManaged = false, currentCourseTab = true;
    window.setPlanningMode = function (editing) {
        if (!circle || !circleOutline) return;
        circle.setOptions({ strokeWeight: editing ? 2.5 : 1.5, strokeOpacity: editing ? 0.85 : 0.25 });
        circleOutline.setOptions({ strokeWeight: editing ? 6 : 3, strokeOpacity: editing ? 0.95 : 0.35 });
    };
    const placeLinkCache = new Map();
    function normalizedName(name) {
        return String(name || '').normalize('NFKC').toLowerCase()
            .replace(/\((주|유)\)|주식회사|유한회사/g, '').replace(/[^0-9a-z가-힣]/g, '');
    }
    function resolvePlaceUrl(place) {
        if (!places) return Promise.resolve(null);
        const key = JSON.stringify([place.name, place.lat, place.lng]);
        if (placeLinkCache.has(key)) return placeLinkCache.get(key);
        const lookup = new Promise(function (resolve) {
            let finished = false;
            let triedCategory = false;
            const timer = setTimeout(function () { finish(null); }, 5000);
            function finish(url) {
                if (finished) return;
                finished = true; clearTimeout(timer); resolve(url);
            }
            function unmatched() {
                const category = ({ MEAL: 'FD6', CAFE: 'CE7' })[place.category];
                if (!triedCategory && category && places.categorySearch) {
                    triedCategory = true;
                    try {
                        places.categorySearch(category, handleResults, {
                            location: new kakao.maps.LatLng(place.lat, place.lng), radius: 200,
                            size: 15, sort: kakao.maps.services.SortBy.DISTANCE
                        });
                    } catch (failure) { finish(null); }
                } else finish(null);
            }
            function handleResults(data, resultStatus) {
                    if (finished) return;
                    if (resultStatus !== kakao.maps.services.Status.OK) { unmatched(); return; }
                    // Require matching names and a nearby coordinate; never pick an arbitrary branch.
                    const matches = data.filter(function (candidate) {
                        const expected = normalizedName(place.name), actual = normalizedName(candidate.place_name);
                        // Stored cafe names can omit common brand/region words (e.g. 커피, 부산, 점).
                        function compact(name) { return name.replace(/커피|카페|부산광역시|부산/g, '').replace(/점$/, ''); }
                        const exact = expected === actual;
                        if (!exact && !(compact(expected).length >= 4 && compact(expected) === compact(actual))) return false;
                        const lat = Number(candidate.y), lng = Number(candidate.x);
                        if (!Number.isFinite(lat) || !Number.isFinite(lng)) return false;
                        const dy = (lat - place.lat) * 111320;
                        const dx = (lng - place.lng) * 111320 * Math.cos(place.lat * Math.PI / 180);
                        candidate.matchDistance = Math.hypot(dx, dy);
                        return candidate.matchDistance <= (exact ? 200 : 100);
                    }).sort(function (a, b) { return a.matchDistance - b.matchDistance; });
                    if (!matches.length) { unmatched(); return; }
                    if (matches.length > 1 && matches[1].matchDistance - matches[0].matchDistance < 30) {
                        finish(null); return;
                    }
                    try {
                        const url = new URL(matches[0].place_url);
                        const id = url.pathname.match(/^\/(\d+)\/?$/);
                        finish(url.hostname === 'place.map.kakao.com' && !url.username && !url.password && id
                            && (url.protocol === 'http:' || url.protocol === 'https:')
                            ? 'https://place.map.kakao.com/' + id[1] : null);
                    } catch (failure) { finish(null); }
            }
            try {
                places.keywordSearch(place.name, handleResults, { location: new kakao.maps.LatLng(place.lat, place.lng), radius: 500, size: 15,
                    sort: kakao.maps.services.SortBy.DISTANCE });
            } catch (failure) { finish(null); }
        });
        if (placeLinkCache.size >= 128) placeLinkCache.delete(placeLinkCache.keys().next().value);
        placeLinkCache.set(key, lookup);
        return lookup;
    }
    function highlightVisit(id) {
        if (!renderingResults && currentPanelManaged && !currentCourseTab) {
            resultMarkers = resultMarkers.filter(function (pin) {
                if (pin.coursePin || pin.visitId === id) return true;
                removeResultPin(pin); return false;
            });
            const visits = itinerary.getVisits();
            const index = visits.findIndex(function (p) { return p.visitId === id; });
            if (index >= 0 && !resultMarkers.some(function (pin) { return pin.visitId === id; }))
                createResultPin(visits[index], index, visits, false, true);
        }
        resultMarkers.forEach(function (pin) {
            const active = pin.visitId === id;
            const image = active ? pin.activeImage : pin.normalImage;
            if (pin.displayedImage !== image) { pin.setImage(image); pin.displayedImage = image; }
            pin.setZIndex(active ? 6 : 3);
        });
    }
    let candidateActionsEnabled=true;
    const itinerary = window.createItineraryRail({
        onTabChange: function (tab, items, user) {
            window.showResultPlaces(items, tab === 'course', true, plannerCourse);
            if (user) window.setPlanningMode(false);
        },
        resolvePlaceUrl: resolvePlaceUrl,
        onReport: function(id) {
            if(window.javaMapBridge && typeof window.javaMapBridge.reportPlace==='function')window.javaMapBridge.reportPlace(String(id));
        },
        isCandidate: function(id) {return !!routePresentation && routeLocked && routeCandidates.some(function(p){return p.recordId===id;})
            && !plannerCourse.some(function(p){return p.recordId===id;});},
        canAdd: function(id) {return !!routePresentation && candidateActionsEnabled && routeLocked && routeCandidates.some(function(p){return p.recordId===id;})
            && !plannerCourse.some(function(p){return p.recordId===id;});},
        onAdd: function(id) {if(window.javaMapBridge && typeof window.javaMapBridge.addCandidate==='function')window.javaMapBridge.addCandidate(String(id));},
        onActive: highlightVisit,
        onOpen: function (place, detailBounds, railBounds) {
            placePinPreview.hide();
            if (!ready) return;
            const bounds = document.getElementById('map').getBoundingClientRect();
            const narrow = window.matchMedia('(max-width: 760px)').matches;
            // The Swing/JavaFX option panel lies outside this map viewport already.
            const standalone = !!routePresentation;
            const freeRight = (narrow && !standalone ? railBounds.left : detailBounds.left) - bounds.left - 16;
            const targetX = Math.max(40, freeRight / 2);
            const targetY = narrow ? Math.max(40, (detailBounds.top - bounds.top) / 2) : bounds.height / 2;
            const projection = map.getProjection();
            const point = projection.containerPointFromCoords(new kakao.maps.LatLng(place.lat, place.lng));
            const center = projection.coordsFromContainerPoint(new kakao.maps.Point(
                point.x + (bounds.width / 2 - targetX) * document.getElementById('map').clientWidth / bounds.width,
                point.y + (bounds.height / 2 - targetY) * document.getElementById('map').clientHeight / bounds.height));
            camera.request({intent:'FOCUS', center:center});
        }
    });
    window.setCandidateActionsEnabled=function(enabled){candidateActionsEnabled=enabled;itinerary.refreshActions();};
    window.updatePlannerPlaces = function (course, nearby) {
        routePresentation = null;
        document.getElementById('map-surface').classList.remove('route-presentation');
        if (nextCircle) { nextCircle.setMap(null); nextCircleOutline.setMap(null); }
        document.getElementById('next-range-legend').hidden = true;
        plannerCourse = course;
        itinerary.setCollections(course, nearby);
    };
    window.selectItineraryPlace = function (recordId) { itinerary.openPlace(recordId); };
    window.openRoutePlace = function (recordId) { itinerary.openPlace(recordId); };
    function markerImage(label, origin, active, nearby) {
        const opacity = nearby && !active ? 0.55 : 1;
        const svg = origin
            ? '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="46" viewBox="0 0 48 46"><rect x="2" y="1" width="44" height="22" rx="11" fill="#2563eb" stroke="white" stroke-width="2"/><text x="24" y="16" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="white">출발</text><circle cx="24" cy="35" r="9" fill="white" stroke="white" stroke-width="3"/><circle cx="24" cy="35" r="6" fill="#2563eb"/></svg>'
            : '<svg xmlns="http://www.w3.org/2000/svg" width="38" height="48" viewBox="0 0 38 48"><g opacity="' + opacity + '"><path d="M19 46C15 39 2 28 2 19a17 17 0 1 1 34 0c0 9-13 20-17 27z" fill="#e54870" stroke="white" stroke-width="2"/><text x="19" y="25" text-anchor="middle" font-family="sans-serif" font-size="18" font-weight="bold" fill="white">' + label + '</text></g></svg>';
        // Keep one padded canvas and anchor even when the artwork grows on selection.
        // The explicit rectangle also accepts pointer input just outside the visible pin.
        const artworkWidth = active ? 44 : 38, artworkHeight = active ? 56 : 48;
        const padded = origin ? svg : '<svg xmlns="http://www.w3.org/2000/svg" width="' + PIN_INPUT.width + '" height="' + PIN_INPUT.height + '" viewBox="0 0 ' + PIN_INPUT.width + ' ' + PIN_INPUT.height + '">'
            + svg.replace('<svg ', '<svg x="' + ((PIN_INPUT.width - artworkWidth) / 2) + '" y="' + (PIN_INPUT.anchorY - (active ? 54 : 46)) + '" ')
                .replace('width="38" height="48"', 'width="' + artworkWidth + '" height="' + artworkHeight + '"') + '</svg>';
        const info = {
            src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(padded.replace('#e54870', nearby ? (active || routePresentation ? '#2563eb' : '#64748b') : active ? '#9d174d' : '#e54870')),
            width: origin ? 48 : PIN_INPUT.width, height: origin ? 46 : PIN_INPUT.height,
            x: origin ? 24 : PIN_INPUT.anchorX, y: origin ? 35 : PIN_INPUT.anchorY
        };
        const hitOptions = { offset: new kakao.maps.Point(info.x, info.y) };
        if (!origin) { hitOptions.shape = 'rect'; hitOptions.coords = [0,0,PIN_INPUT.width,PIN_INPUT.height].join(','); }
        const image = new kakao.maps.MarkerImage(info.src, new kakao.maps.Size(info.width, info.height), hitOptions);
        return image;
    }
    // One preview owns its overlay and lifecycle; hovering never selects a place or route.
    const placePinPreview = (function () {
        let overlay = null, body = null, activePin = null, leaveTimer = null, pointer = null;
        const gesture = new window.PinPointerGesture(PIN_INPUT);
        function hide(pin) {
            if (pin && pin !== activePin) return;
            clearTimeout(leaveTimer); leaveTimer = null;
            activePin = null;
            if (overlay) overlay.setMap(null);
        }
        function scheduleLeave(pin) {
            if (!pin || pin !== activePin || leaveTimer !== null
                    || gesture.heldPin === pin) return;
            leaveTimer = setTimeout(function () { hide(pin); }, PIN_INPUT.leaveDelay);
        }
        function hitTest(pointer) {
            if (!pointer) return null;
            const viewport = document.getElementById('map'), rect = viewport.getBoundingClientRect();
            if (!ready || pointer.blocked || !rect.width || !rect.height || pointer.x < rect.left || pointer.x > rect.right
                    || pointer.y < rect.top || pointer.y > rect.bottom) return null;
            const x = (pointer.x - rect.left) * viewport.clientWidth / rect.width;
            const y = (pointer.y - rect.top) * viewport.clientHeight / rect.height;
            const projection = map.getProjection();
            let nearest = null, active = null;
            resultMarkers.forEach(function (pin) {
                if (!pin.coursePin || !pin.previewPlace) return;
                const place = pin.previewPlace;
                const point = projection.containerPointFromCoords(new kakao.maps.LatLng(place.lat, place.lng));
                const dx = x - point.x, dy = y - point.y;
                const left = -PIN_INPUT.anchorX, right = PIN_INPUT.width - PIN_INPUT.anchorX;
                const top = -PIN_INPUT.anchorY, bottom = PIN_INPUT.height - PIN_INPUT.anchorY, margin = PIN_INPUT.exitMargin;
                const centerX = (left+right)/2, centerY = (top+bottom)/2;
                const match = {pin:pin, score:(dx-centerX)**2 + (dy-centerY)**2,
                    inside:dx>=left && dx<=right && dy>=top && dy<=bottom,
                    nearby:dx>=left-margin && dx<=right+margin && dy>=top-margin && dy<=bottom+margin};
                if (pin === activePin) active = match;
                if (match.inside && (!nearest || match.score < nearest.score)) nearest = match;
            });
            // A small exit margin and tie allowance prevent boundary noise without trapping nearby pins.
            if (active && active.inside && nearest && active.score <= nearest.score + PIN_INPUT.tieTolerance**2) nearest = active;
            if (!nearest && active && active.nearby) nearest = active;
            return nearest ? nearest.pin : null;
        }
        function reconcilePointer() {
            if (gesture.heldPin) return;
            const pin = hitTest(pointer);
            if (pin) show(pin, pin.previewPlace, pin.previewLabel); else scheduleLeave(activePin);
        }
        function pointerFrom(event) {
            const target = event.target;
            return {x:event.clientX, y:event.clientY, blocked:event.type === 'mousemove' && !!event.buttons || !!(target.closest
                && target.closest('.place-detail,.itinerary-rail,#place-search,#walking-panel,.walking-route-tooltip,.walking-leg-badge'))};
        }
        function enter(pin, place, label) {
            if (resultMarkers.indexOf(pin) < 0 || gesture.heldPin) return;
            if (pointer) reconcilePointer(); else show(pin, place, label);
        }
        function leave(pin) {
            if (pointer) reconcilePointer(); else scheduleLeave(pin);
        }
        // Capture real pointer coordinates before SDK child elements synthesize enter/exit events.
        document.addEventListener('mousemove', function (event) {
            gesture.move({x:event.clientX, y:event.clientY});
            pointer = pointerFrom(event);
            reconcilePointer();
        }, true);
        document.addEventListener('mousedown', function (event) {
            if (event.button !== 0) { gesture.reset(); return; }
            const point = pointerFrom(event);
            gesture.begin(point, hitTest(point));
            if (gesture.heldPin) { clearTimeout(leaveTimer); leaveTimer = null; }
        }, true);
        document.addEventListener('mouseup', function (event) {
            if (event.button !== 0) return;
            const point = pointerFrom(event);
            const pin = gesture.finish(point, target => !point.blocked && resultMarkers.includes(target));
            // Deliver the command directly; SDK notifications are separately deduplicated by gesture identity.
            if (pin && pin.openDetails) pin.openDetails();
        }, true);
        document.addEventListener('click', function (event) {
            if (event.button !== 0 || event.defaultPrevented || event.detail === 0 && event.clientX === 0 && event.clientY === 0) return;
            const point = pointerFrom(event);
            if (gesture.consumesClick(point)) { event.preventDefault(); event.stopImmediatePropagation(); return; }
            const dragged = gesture.isDragAt(point); gesture.cancelPress();
            if (dragged) return;
            const pin = hitTest(point);
            if (!pin || !pin.openDetails) return;
            event.preventDefault(); event.stopImmediatePropagation(); pin.openDetails();
        }, true);
        document.addEventListener('keydown', function () { gesture.reset(); }, true);
        document.documentElement.addEventListener('mouseleave', function () { pointer = null; gesture.cancelPress(); scheduleLeave(activePin); });
        window.addEventListener('blur', function () { pointer = null; gesture.reset(); hide(); });
        function show(pin, place, label) {
            if (!ready || resultMarkers.indexOf(pin) < 0) return;
            clearTimeout(leaveTimer); leaveTimer = null;
            if (activePin === pin) return;
            if (!body) {
                body = document.createElement('div'); body.className = 'place-pin-preview';
                body.setAttribute('role', 'tooltip');
                overlay = new kakao.maps.CustomOverlay({ content: body, xAnchor: .5, yAnchor: 1, zIndex: 45 });
            }
            activePin = pin; body.textContent = '';
            const eyebrow = document.createElement('div'); eyebrow.className = 'place-pin-preview-category';
            eyebrow.textContent = label + '번째 장소';
            const title = document.createElement('strong'); title.className = 'place-pin-preview-title'; title.textContent = place.name;
            const facts = document.createElement('div'); facts.className = 'place-pin-preview-facts';
            const presentation = place.presentation || {};
            facts.textContent = presentation.priceText || place.priceText || '가격 미확인';
            const badges = (presentation.badges || []).slice(0, 2).map(function (badge) { return badge.label; });
            if (badges.length) facts.textContent += ' · ' + badges.join(' · ');
            const hint = document.createElement('div'); hint.className = 'place-pin-preview-hint';
            hint.textContent = '클릭해서 장소 상세 보기';
            body.appendChild(eyebrow); body.appendChild(title); body.appendChild(facts); body.appendChild(hint);
            body.style.transform = 'translateY(-54px)';
            overlay.setPosition(new kakao.maps.LatLng(place.lat, place.lng)); overlay.setMap(map);
            // Keep the preview readable at map edges without panning or changing selection.
            const point = map.getProjection().containerPointFromCoords(new kakao.maps.LatLng(place.lat, place.lng));
            const width = body.offsetWidth || 240, height = body.offsetHeight || 110;
            const viewport = document.getElementById('map');
            const shiftX = Math.max(12 + width / 2 - point.x, Math.min(0, viewport.clientWidth - 12 - width / 2 - point.x));
            const preferredY = point.y < height + 66 ? height + 12 : -54;
            const shiftY = Math.max(12 + height - point.y, Math.min(viewport.clientHeight - 12 - point.y, preferredY));
            body.style.transform = 'translate(' + shiftX + 'px,' + shiftY + 'px)';
        }
        return {enter: enter, hide: hide, leave: leave, shouldIgnoreClick: pin => gesture.blocksSdkClick(pin)};
    }());
    function removeResultPin(pin) {
        placePinPreview.hide(pin);
        [['click', pin.visitClick], ['mouseover', pin.visitEnter], ['mouseout', pin.visitLeave]].forEach(function (event) {
            if (event[1]) kakao.maps.event.removeListener(pin, event[0], event[1]);
        });
        pin.setMap(null);
    }
    function createResultPin(item, index, visits, connectRoute, panelManaged) {
            const point = new kakao.maps.LatLng(item.lat, item.lng);
            const courseIndex = plannerCourse.findIndex(function (p) { return p.placeId ? p.placeId === item.placeId : p.recordId === item.recordId; });
            const nearby = (panelManaged || routePresentation) && !connectRoute && courseIndex < 0;
            const candidateRank = routeCandidates.findIndex(function (p) { return p.recordId === item.recordId; }) + 1;
            const label = nearby ? (routePresentation ? candidateRank : '·') : panelManaged && !connectRoute ? courseIndex + 1 : index + 1;
            const pin = new kakao.maps.Marker({ position: point, title: (nearby ? '추천 후보' : label) + '. ' + item.name,
                image: markerImage(label, false, false, nearby), zIndex: 3 });
            pin.setMap(map);
            pin.normalImage = markerImage(label, false, false, nearby);
            pin.activeImage = markerImage(label, false, true, nearby);
            if (connectRoute || panelManaged || routePresentation) {
                pin.visitId = visits[index].visitId;
                pin.openDetails = function () {
                    if (!resultMarkers.includes(pin)) return;
                    claimMapObjectClick();
                    placePinPreview.hide();
                    window.dismissWalkingPopup();
                    if (routePresentation && nearby && window.javaMapBridge && typeof window.javaMapBridge.onPlaceSelected === 'function')
                        window.javaMapBridge.onPlaceSelected(String(item.recordId));
                    itinerary.openVisit(pin.visitId);
                };
                pin.visitClick = function () { if (!placePinPreview.shouldIgnoreClick(pin)) pin.openDetails(); };
                kakao.maps.event.addListener(pin, 'click', pin.visitClick);
            }
            pin.coursePin = courseIndex >= 0;
            if (pin.coursePin) {
                pin.previewPlace = item; pin.previewLabel = courseIndex + 1;
                pin.visitEnter = function () { placePinPreview.enter(pin, item, courseIndex + 1); };
                pin.visitLeave = function () { placePinPreview.leave(pin); };
                kakao.maps.event.addListener(pin, 'mouseover', pin.visitEnter);
                kakao.maps.event.addListener(pin, 'mouseout', pin.visitLeave);
            }
            resultMarkers.push(pin);
            return pin;
    }
    window.showResultPlaces = function (items, connectRoute, panelManaged, routeItems) {
        const signature = JSON.stringify([items, !!connectRoute, routeItems, selectedPosition && [selectedPosition.getLat(), selectedPosition.getLng()]]);
        if (signature === resultSignature) return;
        resultSignature = signature;
        placePinPreview.hide();
        resultMarkers.forEach(removeResultPin);
        resultMarkers = [];
        if (!ready) return;
        renderingResults = true;
        currentPanelManaged = !!panelManaged; currentCourseTab = !!connectRoute;
        const visits = routePresentation ? itinerary.setStandaloneItems(items) : itinerary.setItems(connectRoute || panelManaged ? items : []);
        items.forEach(function (item, index) {
            if (!panelManaged || connectRoute || plannerCourse.some(function (p) { return p.placeId ? p.placeId === item.placeId : p.recordId === item.recordId; }))
                createResultPin(item, index, visits, connectRoute, panelManaged);
        });
        renderingResults = false;
        highlightVisit(itinerary.getState().activePlaceId);
        const route = routeItems || (connectRoute ? items : []);
        const routeKey = JSON.stringify([route, selectedPosition && [selectedPosition.getLat(), selectedPosition.getLng()]]);
        if (routeKey === renderedRouteKey) return;
        renderedRouteKey = routeKey;
    };
    window.focusResultPlace = function (lat, lng) {
        if (ready) camera.request({intent:'FOCUS', center:new kakao.maps.LatLng(lat, lng)});
    };

    // 메인 지도와 편집 지도는 각 WebView 안에 이 상태를 별도로 보관한다.
    // A temporary presentation of this same SDK map; never downloads or stitches tiles.
    let courseCapture = null;
    window.beginCourseCapture = function(plan) {
        if (!ready || courseCapture) return false;
        camera.cancel();
        if (window.disposeMapZoom && window.disposeMapZoom.cancel) window.disposeMapZoom.cancel();
        const state = {center:map.getCenter(), level:map.getLevel(), hidden:[], overlays:[], started:performance.now()};
        courseCapture = state;
        [marker,circle,circleOutline,nextCircle,nextCircleOutline].concat(resultMarkers,walkingShapes).forEach(function(shape){
            if (!shape) return;
            state.hidden.push([shape,shape.getMap()]); shape.setMap(null);
        });
        placePinPreview.hide();
        document.body.classList.add('course-capture');
        map.relayout();
        const stops = [{lat:plan.startLat,lng:plan.startLng,name:'출발지'}].concat(plan.places);
        const path = stops.map(function(p){return new kakao.maps.LatLng(p.lat,p.lng);});
        const bounds = new kakao.maps.LatLngBounds(); path.forEach(function(p){bounds.extend(p);});
        [ {strokeWeight:7,strokeColor:'#ffffff'}, {strokeWeight:4,strokeColor:'#f05475'} ].forEach(function(style){
            const line = new kakao.maps.Polyline(Object.assign({map:map,path:path,strokeOpacity:1,zIndex:10},style));
            state.overlays.push(line);
        });
        stops.forEach(function(stop,i){
            const label = document.createElement('div'); label.className='course-capture-pin';
            const number = document.createElement('b'); number.textContent=i===0?'S':String(i);
            if(i===0)label.classList.add('is-origin');
            const name=document.createElement('span');name.textContent=stop.name;
            label.appendChild(number);label.appendChild(name);
            const overlay=new kakao.maps.CustomOverlay({position:path[i],content:label,yAnchor:i===0?-.25:1.25,zIndex:20+i});
            overlay.setMap(map);state.overlays.push(overlay);
        });
        state.tileTime=0;
        state.tilesLoaded=function(){state.tileTime=performance.now();};
        kakao.maps.event.addListener(map,'tilesloaded',state.tilesLoaded);
        requestAnimationFrame(function(){requestAnimationFrame(function(){
            if(courseCapture!==state)return;
            if(typeof map.jump==='function')map.jump(map.getCenter(),map.getLevel(),{animate:false});
            map.relayout();map.setBounds(bounds,85,155,85,155);
            if(map.getLevel()<2)map.setLevel(2,{animate:false});
            state.fitted=performance.now();
        });});
        return true;
    };
    window.courseCaptureReady = function() {
        const state=courseCapture;
        if (!state || !state.fitted) return false;
        const images=Array.from(document.querySelectorAll('#map img[src]'));
        if(!images.length || images.some(function(img){return !img.complete || !img.naturalWidth;})) {
            state.stableFrame=null;state.stableSince=performance.now();return false;
        }
        // Cached tiles may not emit a new tilesloaded event when fitting leaves the viewport unchanged.
        const hasTiles=images.some(function(img){return img.naturalWidth>=128 && img.naturalHeight>=128;});
        if(!state.tileTime && !hasTiles)return false;
        const center=map.getCenter();
        const frame=JSON.stringify([map.getLevel(),center.getLat(),center.getLng(),
            document.getElementById('map').clientWidth,document.getElementById('map').clientHeight,
            images.map(function(img){return img.currentSrc || img.src;})]);
        if(state.stableFrame!==frame) {
            state.stableFrame=frame;state.stableSince=performance.now();return false;
        }
        return performance.now()-Math.max(state.fitted,state.tileTime||0,state.stableSince)>=400;
    };
    window.endCourseCapture = function() {
        if(!courseCapture)return;
        const state=courseCapture;courseCapture=null;
        kakao.maps.event.removeListener(map,'tilesloaded',state.tilesLoaded);
        state.overlays.forEach(function(shape){shape.setMap(null);});
        state.hidden.forEach(function(item){item[0].setMap(item[1]);});
        document.body.classList.remove('course-capture');
        map.relayout();map.setLevel(state.level);map.setCenter(state.center);
    };

    window.presentRoute = function (plan, candidate, radius, locked, fit, candidates) {
        if (!ready) return;
        const originChanged = routePresentation && (routePresentation.startLat !== plan.startLat || routePresentation.startLng !== plan.startLng);
        routePresentation = plan;
        routeCandidate = candidate;
        routeCandidates = candidates || (candidate ? [candidate] : []);
        document.getElementById('map-surface').classList.add('route-presentation');
        routeLocked = locked;
        plannerCourse = plan.places;
        document.getElementById('place-search').hidden = locked;
        if (!selectedPosition || selectedPosition.getLat() !== plan.startLat || selectedPosition.getLng() !== plan.startLng) {
            suppressLocationNotification = true;
            try { window.selectMapLocation(plan.startLat, plan.startLng, '출발 위치'); }
            finally { suppressLocationNotification = false; }
        }
        window.setMapRadius(plan.radius || radius);
        const center = new kakao.maps.LatLng(plan.startLat, plan.startLng);
        circle.setPosition(center);
        circleOutline.setPosition(center);
        document.getElementById('route-range-label').textContent = '전체 범위 · ' + radiusMeter + 'm';
        const nextLegend = document.getElementById('next-range-legend');
        nextLegend.hidden = !locked;
        if (locked) {
            nextRadiusMeter = Number.isFinite(radius) ? Math.max(200, Math.min(2000, radius)) : 500;
            const last = plan.places.length ? plan.places[plan.places.length - 1] : {lat:plan.startLat, lng:plan.startLng};
            const nextCenter = new kakao.maps.LatLng(last.lat, last.lng);
            displayedNextRange = {lat:last.lat, lng:last.lng, radius:nextRadiusMeter};
            if (!nextCircle) {
                nextCircleOutline = new kakao.maps.Circle({
                    center: nextCenter, radius: nextRadiusMeter,
                    strokeWeight: 5, strokeColor: '#ffffff', strokeOpacity: 0.95,
                    strokeStyle: 'shortdash', fillOpacity: 0, zIndex: 0
                });
                nextCircle = new kakao.maps.Circle({
                    center: nextCenter, radius: nextRadiusMeter,
                    strokeWeight: 2.5, strokeColor: '#15803d', strokeOpacity: 0.95,
                    strokeStyle: 'shortdash', fillOpacity: 0, zIndex: 1
                });
            }
            [nextCircleOutline, nextCircle].forEach(function (shape) {
                shape.setPosition(nextCenter); shape.setRadius(nextRadiusMeter); shape.setMap(map);
            });
            document.getElementById('next-range-label').textContent =
                (plan.places.length ? '이전 장소' : '출발지') + ' 기준 · ' + nextRadiusMeter + 'm';
        } else if (nextCircle) {
            nextCircle.setMap(null); nextCircleOutline.setMap(null);
        }
        const rankedCandidates = routeCandidates.map(function (place, index) { return Object.assign({}, place, {candidateRank:index + 1}); });
        window.showResultPlaces(plan.places.concat(rankedCandidates), false, false, plan.places);
        if (candidate) itinerary.activatePlace(candidate.recordId);
        document.getElementById('itinerary-rail').hidden = true;
        status.textContent = locked ? '분홍 번호는 코스 순서, 옅은 파랑 번호는 후보 순위입니다. 경로에 마우스를 올리면 구간 안내를 볼 수 있어요.' : '확정한 코스 · 경로에 마우스를 올리면 구간 안내를 볼 수 있어요.';
        if (fit) window.fitRouteView(false, originChanged ? 'FOCUS' : null);
    };

    window.focusRouteLeg = function (index, radius) {
        if (!routePresentation || !routeLocked || !nextCircle) return;
        const plan = routePresentation;
        const editing = Number.isInteger(index) && index >= 0 && index < plan.places.length;
        const previous = editing ? (index ? plan.places[index - 1] : {lat:plan.startLat,lng:plan.startLng,name:'출발지'})
            : (plan.places.length ? plan.places[plan.places.length - 1] : {lat:plan.startLat,lng:plan.startLng,name:'출발지'});
        const limit = editing && Number.isFinite(radius) ? radius : nextRadiusMeter;
        displayedNextRange = {lat:previous.lat, lng:previous.lng, radius:limit};
        [nextCircle, nextCircleOutline].forEach(function(shape) {
            shape.setPosition(new kakao.maps.LatLng(previous.lat, previous.lng));
            shape.setRadius(limit);
        });
        document.getElementById('next-range-label').textContent = editing
            ? previous.name + ' → ' + plan.places[index].name + ' · ' + limit + 'm'
            : (plan.places.length ? '마지막 장소' : '출발지') + ' 기준 · ' + limit + 'm';
        const focused = editing ? plan.places[index] : routeCandidate || plan.places[plan.places.length - 1];
        const visit = focused && itinerary.getVisits().find(function(item) { return item.recordId === focused.recordId; });
        highlightVisit(visit ? visit.visitId : null);
    };

    window.resetPlannerMap = function () {
        dismissSearch();
        searchInput.value = '';
        locationVersion++;
        clearTimeout(locationNameTimer);
        selectedPosition = null;
        selectedPlaceName = '';
        originLocked = false;
        routeLocked = false;
        routePresentation = null;
        routeCandidate = null;
        routeCandidates = [];
        plannerCourse = [];
        radiusMeter = nextRadiusMeter = 500;
        displayedNextRange = null;
        resultSignature = renderedRouteKey = '';
        window.showResultPlaces([], false, false, []);
        itinerary.dismissDetail();
        window.showWalkingRoute(null, '', false);
        walkingExplanationOpen = false;
        document.getElementById('walking-panel').hidden = true;
        document.getElementById('next-range-legend').hidden = true;
        document.getElementById('route-range-label').textContent = '출발지 중심 전체 범위';
        [marker, circle, circleOutline, nextCircle, nextCircleOutline].forEach(function(shape) {
            if (shape) shape.setMap(null);
        });
        searchForm.hidden = false;
        if (ready) {
            rangeAdjusting = false;
            camera.request({intent:'RESET', center:new kakao.maps.LatLng(35.1796, 129.0756), level:5});
        }
        status.textContent = '지도에서 출발 위치를 클릭해 주세요.';
    };

    function rangePoints() {
        const plan = routePresentation;
        let points = circlePoints(plan.startLat, plan.startLng, radiusMeter);
        if (routeLocked && displayedNextRange) {
            const range = displayedNextRange;
            points = points.concat(circlePoints(range.lat, range.lng, range.radius));
        }
        return points;
    }
    window.ensureRouteRangesVisible = function (adjusting) {
        if (!ready || !routePresentation) return;
        camera.request({intent:adjusting ? 'RANGE_DRAG' : 'RANGE_COMMIT', points:rangePoints(), keepCenter:true});
    };
    window.fitRouteView = function (wholeRoute, intent) {
        if (!ready || !routePresentation) return;
        if (window.resetMapZoom) window.resetMapZoom();
        const plan = routePresentation;
        const points = wholeRoute ? [new kakao.maps.LatLng(plan.startLat, plan.startLng)].concat(
            plan.places.concat(routeCandidates).map(function (p) {return new kakao.maps.LatLng(p.lat, p.lng);})) : rangePoints();
        camera.request({intent:intent || (rangeAdjusting ? 'RANGE_DRAG' : 'FIT'), points:points});
    };

    let walkingShapes = [];
    let walkingListeners = [];
    let walkingExplanationOpen = false;
    let walkingSelection = 0;
    let walkingPinnedSelection = -1;
    let mapObjectClick = false;
    let walkingPopupHide = null;
    function walkingMidpoint(paths) {
        const segments = [];
        let total = 0;
        const radians = Math.PI / 180;
        (paths || []).forEach(function(path) {
            for (let i = 1; i < path.length; i++) {
                const a = path[i - 1], b = path[i];
                const lat = (b.lat - a.lat) * radians, lng = (b.lng - a.lng) * radians;
                const haversine = Math.sin(lat / 2) ** 2 + Math.cos(a.lat * radians)
                    * Math.cos(b.lat * radians) * Math.sin(lng / 2) ** 2;
                const length = 2 * 6371000 * Math.asin(Math.sqrt(Math.min(1, haversine)));
                if (length > 0) { segments.push({a:a, b:b, length:length}); total += length; }
            }
        });
        let remaining = total / 2;
        for (const segment of segments) {
            if (remaining <= segment.length) {
                const fraction = remaining / segment.length;
                return new kakao.maps.LatLng(segment.a.lat + (segment.b.lat - segment.a.lat) * fraction,
                    segment.a.lng + (segment.b.lng - segment.a.lng) * fraction);
            }
            remaining -= segment.length;
        }
        return null;
    }
    function claimMapObjectClick() {
        mapObjectClick = true;
        if (kakao.maps.event.preventMap) kakao.maps.event.preventMap();
        setTimeout(function () { mapObjectClick = false; }, 0);
    }
    window.dismissWalkingPopup = function () { return false; };
    window.selectWalkingLeg = function () { return false; };
    window.toggleWalkingExplanation = function () {
        walkingExplanationOpen = !walkingExplanationOpen;
        document.getElementById('walking-panel').hidden = !walkingExplanationOpen;
    };
    window.showWalkingRoute = function (route, message, fit) {
        clearTimeout(walkingPopupHide);
        window.selectWalkingLeg = function () { return false; };
        walkingListeners.forEach(function (entry) { kakao.maps.event.removeListener(entry[0], entry[1], entry[2]); });
        walkingListeners = [];
        walkingShapes.forEach(function (shape) { shape.setMap(null); });
        walkingShapes = [];
        const panel = document.getElementById('walking-panel');
        const summary = document.getElementById('walking-summary');
        const details = document.getElementById('walking-details');
        const directions = document.getElementById('walking-directions');
        const legList = document.getElementById('walking-leg-list');
        directions.textContent = '';
        legList.textContent = '';
        panel.hidden = !walkingExplanationOpen;
        details.hidden = !route;
        if (!route || !ready) {
            walkingSelection = 0;
            walkingPinnedSelection = -1;
            window.dismissWalkingPopup = function () { return false; };
            summary.textContent = message || '도보 경로를 계산해 주세요.';
            return;
        }
        // Draw only the actual TMAP walking geometry on the Kakao map.
        const boundsPoints = [];
        let points = 0;
        function meters(value) { return value >= 1000 ? (value / 1000).toFixed(2) + 'km' : value + 'm'; }
        function duration(seconds) { return seconds === 0 ? '0분' : '약 ' + Math.ceil(seconds / 60) + '분'; }
        const colors = ['#2563eb', '#b45309', '#15803d', '#9333ea', '#be185d', '#0e7490'];
        const groups = [];
        let selected = Math.min(walkingSelection, Math.max(0, route.legs.length - 1));
        let emphasized = -1;
        const popupBody = document.createElement('div');
        popupBody.className = 'walking-route-tooltip';
        const popupTitle = document.createElement('div');
        popupTitle.className = 'walking-tooltip-title';
        popupTitle.setAttribute('role', 'heading');
        popupTitle.setAttribute('aria-level', '3');
        const popupFacts = document.createElement('div');
        popupFacts.className = 'walking-tooltip-facts';
        const popupHint = document.createElement('div');
        popupHint.className = 'walking-tooltip-hint';
        popupHint.textContent = '클릭하면 상세 이동 안내';
        const popupClose = document.createElement('button');
        popupClose.className = 'walking-tooltip-close';
        popupClose.type = 'button';
        popupClose.textContent = '×';
        popupClose.setAttribute('aria-label', '경로 안내 닫기');
        const popupSteps = document.createElement('ol');
        popupSteps.className = 'walking-tooltip-steps';
        popupSteps.tabIndex = 0;
        popupSteps.setAttribute('aria-label','이 구간의 도보 안내');
        popupBody.appendChild(popupTitle);
        popupBody.appendChild(popupFacts);
        popupBody.appendChild(popupHint);
        popupBody.appendChild(popupClose);
        popupBody.appendChild(popupSteps);
        const popup = new kakao.maps.CustomOverlay({content:popupBody, yAnchor:1, zIndex:40});
        let popupVisible = false;
        let popupIndex = -1;
        walkingShapes.push(popup);
        window.dismissWalkingPopup = function () {
            clearTimeout(walkingPopupHide);
            const visible = popupVisible;
            walkingPinnedSelection = -1;
            popup.setMap(null);
            popupVisible = false;
            return visible;
        };
        function showPopup(index, position) {
            clearTimeout(walkingPopupHide);
            const detailed = walkingPinnedSelection >= 0;
            if (detailed && index !== walkingPinnedSelection) return;
            popupBody.classList.toggle('is-detailed', detailed);
            popupBody.setAttribute('aria-label', detailed ? '구간 상세 이동 안내' : '구간 요약');
            popupSteps.hidden = !detailed;
            popupClose.hidden = !detailed;
            popupHint.hidden = detailed;
            const leg = route.legs[index];
            if (popupIndex !== index) {
                popupIndex = index;
                popupTitle.textContent = (index + 1) + '구간 · ' + leg.from.name + ' → ' + leg.to.name;
                popupFacts.textContent = meters(leg.distance) + ' · ' + duration(leg.time);
                popupSteps.textContent = '';
                (leg.steps || []).forEach(function(step) {
                    if(!step.description)return;
                    const item=document.createElement('li');item.textContent=step.description;popupSteps.appendChild(item);
                });
                if(!popupSteps.children.length){const item=document.createElement('li');item.textContent='상세 안내가 없어요.';popupSteps.appendChild(item);}
                popupSteps.scrollTop = 0;
                popupBody.scrollTop = 0;
            }
            const surface = document.getElementById('map');
            const width = Math.max(1, Math.min(detailed ? 360 : 280, surface.clientWidth - 24));
            popupBody.style.width = width + 'px';
            popupBody.style.maxHeight = Math.max(1, Math.min(420, surface.clientHeight - 24)) + 'px';
            popup.setMap(map);
            popupVisible = true;
            const projection = map.getProjection();
            const point = projection.containerPointFromCoords(position || groups[index].anchor);
            const height = popupBody.offsetHeight || 80;
            popup.setPosition(projection.coordsFromContainerPoint(new kakao.maps.Point(
                Math.max(width / 2 + 12, Math.min(surface.clientWidth - width / 2 - 12, point.x)),
                Math.max(height + 12, Math.min(surface.clientHeight - 12, point.y - 12)))));
        }
        function restorePopup() {
            clearTimeout(walkingPopupHide);
            if (walkingPinnedSelection >= 0) return;
            walkingPopupHide=setTimeout(function(){
                if(walkingPinnedSelection>=0)showPopup(walkingPinnedSelection);
                else {popup.setMap(null);popupVisible=false;}
            },120);
        }
        popupBody.addEventListener('mouseenter',function(){clearTimeout(walkingPopupHide);});
        popupBody.addEventListener('mouseleave',restorePopup);
        popupBody.addEventListener('focusin',function(){clearTimeout(walkingPopupHide);});
        popupBody.addEventListener('focusout',restorePopup);
        ['mousedown','pointerdown','wheel','touchstart'].forEach(function(type){popupBody.addEventListener(type,function(e){e.stopPropagation();});});
        popupBody.addEventListener('click',function(e){
            claimMapObjectClick();e.stopPropagation();
            if (walkingPinnedSelection < 0) choose(popupIndex);
        });
        popupClose.addEventListener('click', function(e) {
            claimMapObjectClick();e.stopPropagation();window.dismissWalkingPopup();
        });
        popupBody.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') { e.stopPropagation();window.dismissWalkingPopup(); }
        });
        function listen(target, event, callback) {
            kakao.maps.event.addListener(target, event, callback);
            walkingListeners.push([target, event, callback]);
        }
        function emphasize(index) {
            groups.forEach(function (group, number) { group.button.setAttribute('aria-pressed', String(number === selected)); });
            if (emphasized === index) return;
            emphasized = index;
            groups.forEach(function (group, number) {
                const active = number === index;
                group.lines.forEach(function (line) {
                    line.setOptions({strokeWeight:active ? 6 : 4, strokeOpacity:active ? 1 : 0.55});
                    line.setZIndex(active ? 12 : 4);
                });
                group.outlines.forEach(function (line) { line.setZIndex(active ? 11 : 3); });
            });
        }
        function choose(index, position, pinPopup) {
            if (!Number.isInteger(index) || index < 0 || index >= groups.length) return false;
            selected = index;
            walkingSelection = index;
            emphasize(index);
            const leg = route.legs[index];
            summary.textContent = (index + 1) + '구간 · ' + leg.from.name + ' → ' + leg.to.name
                + ' · ' + meters(leg.distance) + ' · ' + duration(leg.time);
            Array.from(directions.children).forEach(function (item, number) {
                item.hidden = number !== index;
            });
            if (pinPopup !== false && groups[index].anchor) {
                walkingPinnedSelection = index;
                claimMapObjectClick();
                showPopup(index, position);
            }
            return true;
        }
        summary.textContent = 'TMAP 도보 · ' + meters(route.totalDistance) + ' · ' + duration(route.totalTime)
            + ' · ' + route.legs.length + '구간';
        route.legs.forEach(function (leg, index) {
            const color = colors[index % colors.length];
            const caption = (index + 1) + '구간 · ' + leg.from.name + ' → ' + leg.to.name;
            const facts = meters(leg.distance) + ' · ' + duration(leg.time);
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'walking-leg-button';
            button.style.borderLeftColor = color;
            button.textContent = (index + 1) + ' · ' + leg.to.name + ' · ' + facts;
            button.title = caption + ' · ' + facts;
            button.setAttribute('aria-label', button.title);
            button.addEventListener('click', function () { choose(index); });
            button.addEventListener('mouseenter', function () { emphasize(index); });
            button.addEventListener('mouseleave', function () { emphasize(selected); });
            legList.appendChild(button);
            const group = {lines:[], outlines:[], button:button};
            groups.push(group);
            let badgePosition = null;
            leg.paths.forEach(function (coordinates) {
                const path = coordinates.map(function (point) {
                    const position = new kakao.maps.LatLng(point.lat, point.lng);
                    boundsPoints.push(position); points++;
                    return position;
                });
                if (path.length < 2) return;
                if (!badgePosition) badgePosition = path[Math.floor(path.length / 2)];
                const outline = new kakao.maps.Polyline({path:path, strokeWeight:7, strokeColor:'#ffffff',
                    strokeOpacity:0.95, strokeStyle:'solid', zIndex:3});
                const line = new kakao.maps.Polyline({path:path, strokeWeight:4, strokeColor:color,
                    strokeOpacity:0.95, strokeStyle:'solid', zIndex:4});
                outline.setMap(map); line.setMap(map);
                walkingShapes.push(outline, line);
                group.lines.push(line);
                group.outlines.push(outline);
                function show(event) {
                    emphasize(index);
                    if (walkingPinnedSelection >= 0) return;
                    showPopup(index, event && event.latLng ? event.latLng : path[0]);
                }
                listen(line, 'mouseover', show);
                listen(line, 'mousemove', show);
                listen(line, 'mouseout', function () {
                    restorePopup();
                    emphasize(selected);
                });
                listen(line, 'click', function (event) { choose(index, event && event.latLng); });
            });
            // The geometric half-distance point, independent of sampling density or path splits.
            group.anchor = badgePosition = walkingMidpoint(leg.paths) || badgePosition;
            if (badgePosition) {
                const badge = document.createElement('button');
                badge.type = 'button';
                badge.className = 'walking-leg-badge';
                badge.textContent = String(index + 1);
                badge.style.backgroundColor = color;
                badge.setAttribute('aria-label', caption + ' · ' + facts);
                function previewBadge() {
                    emphasize(index);
                    if (walkingPinnedSelection < 0) showPopup(index, badgePosition);
                }
                function leaveBadge() {
                    restorePopup();
                    emphasize(selected);
                }
                badge.addEventListener('mouseenter', previewBadge);
                badge.addEventListener('mouseleave', leaveBadge);
                badge.addEventListener('focus', previewBadge);
                badge.addEventListener('blur', leaveBadge);
                badge.addEventListener('click', function () { choose(index, badgePosition); });
                const marker = new kakao.maps.CustomOverlay({position:badgePosition, content:badge,
                    clickable:true, xAnchor:0.5, yAnchor:0.5, zIndex:15});
                marker.setMap(map);
                walkingShapes.push(marker);
            }
            const item = document.createElement('li');
            const instructions = document.createElement('ol');
            leg.steps.forEach(function (step) {
                const instruction = document.createElement('li');
                instruction.textContent = step.description;
                instructions.appendChild(instruction);
            });
            if (leg.steps.length) item.appendChild(instructions);
            else item.textContent = '상세 안내가 없어요.';
            directions.appendChild(item);
        });
        window.selectWalkingLeg = choose;
        choose(selected, null, false);
        if (walkingPinnedSelection >= 0 && groups[walkingPinnedSelection] && groups[walkingPinnedSelection].anchor)
            showPopup(walkingPinnedSelection);
        if (fit && points) camera.request({intent:'FIT', points:boundsPoints});
    };

    const key = window.KAKAO_MAP_KEY && window.KAKAO_MAP_KEY.trim();
    if (!key) {
        reportFailure("kakao-config.js에 JavaScript 키를 입력해 주세요.");
        return;
    }


    const sdk = document.createElement("script");
    let sdkTimedOut = false;
    const sdkTimeout = setTimeout(function () {
        sdkTimedOut = true;
        reportFailure("지도 연결 시간이 초과됐어요. 인터넷 연결을 확인하고 지도 다시 불러오기를 눌러 주세요.");
    }, 20000);
    sdk.src = "https://dapi.kakao.com/v2/maps/sdk.js?appkey=" + encodeURIComponent(key) + "&autoload=false&libraries=services";
    sdk.onerror = function () {
        clearTimeout(sdkTimeout);
        reportFailure("지도 SDK를 불러오지 못했습니다. 인터넷 연결과 카카오맵 설정을 확인해 주세요.");
    };
    sdk.onload = function () {
        if (sdkTimedOut) return;
        if (!window.kakao || !kakao.maps) {
            clearTimeout(sdkTimeout);
            reportFailure("지도 SDK를 초기화하지 못했습니다.");
            return;
        }
        kakao.maps.load(function () {
            if (sdkTimedOut) return;
            clearTimeout(sdkTimeout);
            window.whenMapViewportReady(document.getElementById('map'), function () {
                try {
                    map = new kakao.maps.Map(document.getElementById("map"), {
                        center: new kakao.maps.LatLng(35.1796, 129.0756), level: 5,
                        disableDoubleClickZoom: USE_INSTANT_ZOOM
                    });

                    camera = window.createMapCamera(map, document.getElementById('map'), decideCamera);
                    marker = new kakao.maps.Marker({ image: markerImage('', true), title: '출발 위치 · 탐색 반경 중심', zIndex: 5 });
                    circle = new kakao.maps.Circle({
                        center: map.getCenter(), radius: radiusMeter,
                        strokeWeight: 2.5, strokeColor: "#2563eb", strokeOpacity: 0.85,
                        strokeStyle: "solid", fillOpacity: 0, zIndex: 0
                    });
                    circleOutline = new kakao.maps.Circle({
                        center: map.getCenter(), radius: radiusMeter,
                        strokeWeight: 6, strokeColor: "#ffffff", strokeOpacity: 0.95,
                        strokeStyle: "solid", fillOpacity: 0, zIndex: -1
                    });
                    function select(event) {
                        if (mapObjectClick) return;
                        placePinPreview.hide();
                        const closedWalking = window.dismissWalkingPopup();
                        const closedDetail = itinerary.dismissDetail();
                        if (closedWalking || closedDetail) return;
                        if (routeLocked) return;
                        dismissSearch();
                        window.selectMapLocation(event.latLng.getLat(), event.latLng.getLng());
                    }
                    kakao.maps.event.addListener(map, "click", select);
                    kakao.maps.event.addListener(circle, "click", select);
                    kakao.maps.event.addListener(circleOutline, "click", select);
                    ready = true;
                    if (kakao.maps.services && kakao.maps.services.Places) {
                        places = new kakao.maps.services.Places();
                        searchButton.disabled = false;
                    } else {
                        searchNotice("장소 검색을 불러오지 못했어요. 지도에서 직접 위치를 선택해 주세요.");
                    }
                    if (kakao.maps.services && kakao.maps.services.Geocoder) {
                        geocoder = new kakao.maps.services.Geocoder();
                    }
                    status.textContent = "지도에서 출발 위치를 클릭해 주세요.";
                    window.connectJavaMapBridge();

                } catch (failure) {
                    reportFailure("지도를 표시하지 못했습니다. 앱 또는 페이지를 다시 열어 주세요.");
                }
            });
        });
    };
    document.head.appendChild(sdk);
}());
