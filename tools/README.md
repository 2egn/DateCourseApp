# 앱과 분리해서 사용하는 도구

| 위치 | 용도 |
|---|---|
| `place-data-pilot/` | 서면·전포 공공자료 추출, 가격 규칙 검사, Google Places 소량 조회 실험 |

```powershell
# 프로젝트 최상단에서 실행. 외부 API를 호출하지 않는다.
python -m unittest discover -s tools/place-data-pilot -p test_pilot.py -v
```

실행 방법과 코드 읽는 순서는 `place-data-pilot/README.md`에 있다. 테스트 데이터는 `data/places/pilot/`, 결과 문서는 `docs/research/`, 임시 결과는 `build/place-data-pilot/`에서 찾는다.

실제 API 조회 명령은 별도이며, 이전 실행의 잠금 파일을 유지해 실수로 반복 호출되지 않게 했다. 폴더 정리 과정에서는 API를 다시 호출하지 않는다.

문서 사이트에만 쓰는 빌드·검사 도구는 해당 기능과 함께 `docs/site/tools/`에 둔다.
