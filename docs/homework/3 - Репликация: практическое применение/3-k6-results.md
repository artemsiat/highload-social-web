# Подробный отчет k6
## Mixed read workload: `/user/get/{id}` + `/user/search`

## Параметры теста

- Инструмент: `k6`
- Длительность каждого прогона: `3m`
- Тип нагрузки: mixed read workload
- Сценарий:
  - `GET /user/get/{id}`
  - `GET /user/search`
- Соотношение запросов:
  - `USER_GET_RATIO=0.5`
  - `USER_SEARCH_RATIO=0.5`
- Диапазон `id` для `/user/get/{id}`: `1..999999`
- Режим `/user/search`: `dataset`
- Количество search pairs: `12`

Throughput рассчитан как:

```text
completed iterations / 180s
```

---

## До master/slave

### Общие результаты mixed read test

| VU | Avg throughput, req/s | Complete iterations | Error rate, % | Checks rate, % | Threshold status | JSON |
|---:|---:|---:|---:|---:|---|---|
| 10 | 163.22 | 29393 | 0.0068 | 99.9966 | passed | [JSON](./k6/results/mixed-read-10vus-3m-20260413-220945.json) |
| 100 | 155.93 | 28172 | 0.0035 | 99.9982 | crossed | [JSON](./k6/results/mixed-read-100vus-3m-20260413-221526.json) |
| 500 | 163.66 | 29963 | 0.0067 | 99.9967 | crossed | [JSON](./k6/results/mixed-read-500vus-3m-20260413-221943.json) |

### `GET /user/get/{id}`

| VU | Avg, ms | Median, ms | P90, ms | P95, ms | P99, ms | Max, ms | Threshold `p95 < 500 ms` |
|---:|---:|---:|---:|---:|---:|---:|---|
| 10 | 2.89 | 2.29 | 5.03 | 6.23 | 10.50 | 49.09 | passed |
| 100 | 577.16 | 555.61 | 722.26 | 795.83 | 1100.24 | 2426.86 | failed |
| 500 | 2972.84 | 2965.01 | 3312.97 | 3470.34 | 4143.39 | 5404.82 | failed |

### `GET /user/search`

| VU | Avg, ms | Median, ms | P90, ms | P95, ms | P99, ms | Max, ms | Threshold `p95 < 500 ms` |
|---:|---:|---:|---:|---:|---:|---:|---|
| 10 | 120.02 | 100.68 | 186.99 | 205.32 | 258.84 | 507.39 | passed |
| 100 | 702.29 | 677.11 | 876.38 | 975.23 | 1286.84 | 2774.21 | failed |
| 500 | 3081.13 | 3076.94 | 3433.76 | 3591.57 | 4170.90 | 6171.25 | failed |

### Краткие выводы по baseline

- при `10 VU` оба read-endpoint-а укладываются в целевой threshold по `p95`;
- при `100 VU` threshold `p95 < 500 ms` нарушается уже для обоих endpoint-ов;
- при `500 VU` latency вырастает до нескольких секунд, что указывает на упор в single-node конфигурацию;
- throughput почти не растет между `10`, `100` и `500` VU, что похоже на достижение предельной пропускной способности текущего single DB setup;
- доля ошибок остается очень низкой, поэтому основная проблема — деградация latency, а не отказоустойчивость на уровне HTTP-ответов.
