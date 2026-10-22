(function () {
    "use strict";

    let map;
    let marker;
    let circle;
    let selectedPosition;
    let radiusMeter = 500;
    let ready = false;
    let failureMessage = "";
    const status = document.getElementById("map-status");
    const searchForm = document.getElementById("search-form");
    const searchInput = document.getElementById("place-query");
    const searchButton = document.getElementById("search-submit");
    const searchMessage = document.getElementById("search-message");
    const searchResults = document.getElementById("search-results");
    const searchClose = document.getElementById("search-close");
    let places;
    let searchVersion = 0;
    let searchTimer;

    // Search results take their own layout space; resize the map when that space changes.
    let mapLayoutPending = false;
    new MutationObserver(function () {
        if (!ready || mapLayoutPending) return;
        mapLayoutPending = true;
        window.requestAnimationFrame(function () {
            mapLayoutPending = false;
            const center = map.getCenter();
            map.relayout();
            map.setCenter(center);
        });
    }).observe(document.getElementById("place-search"), {
        childList: true, subtree: true, characterData: true, attributes: true,
        attributeFilter: ["hidden"]
    });

    function clearSearchResults() {
        while (searchResults.firstChild) searchResults.removeChild(searchResults.firstChild);
        searchClose.hidden = true;
    }

    function dismissSearch() {
        searchVersion++;
        clearTimeout(searchTimer);
        clearSearchResults();
        searchMessage.textContent = "";
    }

    function renderSearchResults(results) {
        searchClose.hidden = false;
        results.forEach(function (place) {
            const item = document.createElement("li");
            const button = document.createElement("button");
            button.type = "button";
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
                window.selectMapLocation(latitude, longitude);
                map.setCenter(new kakao.maps.LatLng(latitude, longitude));
                searchInput.value = place.place_name;
                clearSearchResults();
                searchMessage.textContent = "";
                searchInput.focus();
            });
            item.appendChild(button);
            searchResults.appendChild(item);
        });
    }

    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        const version = ++searchVersion;
        clearTimeout(searchTimer);
        clearSearchResults();
        const query = searchInput.value.trim();
        if (!query) {
            searchMessage.textContent = "출발할 장소 이름을 입력해 주세요.";
            searchInput.focus();
            return;
        }
        if (!ready || !places) {
            searchMessage.textContent = "장소 검색이 준비될 때까지 기다려 주세요.";
            return;
        }
        searchMessage.textContent = "부산 지역에서 검색하고 있어요…";
        searchTimer = setTimeout(function () {
            if (version !== searchVersion) return;
            searchVersion++;
            searchMessage.textContent = "검색 시간이 초과됐어요. 다시 검색해 주세요.";
        }, 15000);
        try {
            const keyword = query.indexOf("부산") === -1 ? "부산 " + query : query;
            places.keywordSearch(keyword, function (data, resultStatus) {
                if (version !== searchVersion) return;
                clearTimeout(searchTimer);
                if (resultStatus === kakao.maps.services.Status.ZERO_RESULT) {
                    searchMessage.textContent = "검색 결과가 없어요. 다른 장소 이름을 입력해 주세요.";
                    return;
                }
                if (resultStatus !== kakao.maps.services.Status.OK) {
                    searchMessage.textContent = "검색하지 못했어요. 잠시 후 다시 시도해 주세요.";
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
                    searchMessage.textContent = "부산 지역의 결과가 없어요. 장소 이름을 더 구체적으로 입력해 주세요.";
                    return;
                }
                renderSearchResults(results);
                searchMessage.textContent = "검색 결과 " + results.length + "개 · 출발할 장소를 선택해 주세요.";
            }, { size: 15 });
        } catch (failure) {
            clearTimeout(searchTimer);
            searchMessage.textContent = "검색하지 못했어요. 잠시 후 다시 시도해 주세요.";
        }
    });

    searchInput.addEventListener("input", function () {
        searchVersion++;
        clearTimeout(searchTimer);
        clearSearchResults();
        searchMessage.textContent = "";
    });

    searchClose.addEventListener("click", dismissSearch);
    searchInput.addEventListener("keydown", function (event) {
        if (event.key === "Escape") dismissSearch();
    });

    function reportFailure(message) {
        ready = false;
        failureMessage = message;
        status.textContent = message;
        searchButton.disabled = true;
        searchMessage.textContent = "지도를 불러온 뒤 장소를 검색할 수 있어요.";
        if (window.javaMapBridge) window.javaMapBridge.onMapError(message);
    }

    function notifyLocation() {
        if (selectedPosition && window.javaMapBridge) {
            window.javaMapBridge.onLocationSelected(selectedPosition.getLat(), selectedPosition.getLng());
        }
    }

    // Page load and asynchronous SDK initialization may finish in either order.
    window.connectJavaMapBridge = function () {
        if (!window.javaMapBridge) return;
        if (failureMessage) window.javaMapBridge.onMapError(failureMessage);
        else if (ready) {
            window.javaMapBridge.onMapReady();
            notifyLocation();
        }
    };

    window.setMapRadius = function (value) {
        if (!Number.isFinite(value) || value < 200 || value > 2000) return;
        radiusMeter = value;
        if (circle) circle.setRadius(value);
        updateStatus();
    };

    function updateStatus() {
        if (selectedPosition) {
            status.textContent = "출발 위치 선택 완료 · 반경 " + radiusMeter + "m · 지도를 클릭하면 변경할 수 있어요.";
        }
    }

    window.selectMapLocation = function (latitude, longitude) {
        if (!ready || !Number.isFinite(latitude) || !Number.isFinite(longitude)
            || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
        selectedPosition = new kakao.maps.LatLng(latitude, longitude);
        marker.setPosition(selectedPosition);
        marker.setMap(map);
        circle.setPosition(selectedPosition);
        circle.setRadius(radiusMeter);
        circle.setMap(map);
        updateStatus();
        notifyLocation();
    };

    const key = window.KAKAO_MAP_KEY && window.KAKAO_MAP_KEY.trim();
    if (!key) {
        reportFailure("kakao-config.js에 JavaScript 키를 입력해 주세요.");
        return;
    }


    const sdk = document.createElement("script");
    sdk.src = "https://dapi.kakao.com/v2/maps/sdk.js?appkey=" + encodeURIComponent(key) + "&autoload=false&libraries=services";
    sdk.onerror = function () {
        reportFailure("지도 SDK를 불러오지 못했습니다. 인터넷 연결과 카카오맵 설정을 확인해 주세요.");
    };
    sdk.onload = function () {
        if (!window.kakao || !kakao.maps) {
            reportFailure("지도 SDK를 초기화하지 못했습니다.");
            return;
        }
        kakao.maps.load(function () {
            try {
                map = new kakao.maps.Map(document.getElementById("map"), {
                    center: new kakao.maps.LatLng(35.1796, 129.0756), level: 5
                });
                marker = new kakao.maps.Marker();
                circle = new kakao.maps.Circle({
                    center: map.getCenter(), radius: radiusMeter,
                    strokeWeight: 2, strokeColor: "#FF6B81", strokeOpacity: 0.85,
                    fillColor: "#FF6B81", fillOpacity: 0.16
                });
                function select(event) {
                    dismissSearch();
                    window.selectMapLocation(event.latLng.getLat(), event.latLng.getLng());
                }
                kakao.maps.event.addListener(map, "click", select);
                kakao.maps.event.addListener(circle, "click", select);
                ready = true;
                if (kakao.maps.services && kakao.maps.services.Places) {
                    places = new kakao.maps.services.Places();
                    searchButton.disabled = false;
                } else {
                    searchMessage.textContent = "장소 검색을 불러오지 못했어요. 지도에서 직접 위치를 선택해 주세요.";
                }
                status.textContent = "지도에서 출발 위치를 클릭해 주세요.";
                window.connectJavaMapBridge();

                let resizeTimer;
                window.addEventListener("resize", function () {
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(function () {
                        const center = map.getCenter();
                        map.relayout();
                        map.setCenter(center);
                    }, 80);
                });
            } catch (failure) {
                reportFailure("지도를 표시하지 못했습니다. 앱 또는 페이지를 다시 열어 주세요.");
            }
        });
    };
    document.head.appendChild(sdk);
}());
