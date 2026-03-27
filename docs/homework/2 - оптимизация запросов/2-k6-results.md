# Подробный отчет k6
## `/user/search`

## Параметры теста

- Инструмент: `k6`
- Длительность каждого прогона: `120s`
- Режим: `dataset`
- Количество пар префиксов: `12`

Throughput рассчитан как:

```text
completed iterations / 120s
```

---

## До индекса

| VU | Avg, ms | P95, ms | Max, ms | Complete iterations | Throughput, req/s | JSON |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 34.06 | 41.90 | 116.92 | 3504 | 29.20 | [JSON](./k6/results/user-search-1vus-120s-20260327-104900.json) |
| 10 | 153.66 | 316.17 | 661.86 | 7805 | 65.04 | [JSON](./k6/results/user-search-10vus-120s-20260327-105650.json) |
| 100 | 1373.04 | 1639.65 | 2949.23 | 8793 | 73.28 | [JSON](./k6/results/user-search-100vus-120s-20260327-110343.json) |
| 1000 | 12463.25 | 13332.37 | 15856.91 | 10146 | 84.55 | [JSON](./k6/results/user-search-1000vus-120s-20260327-111304.json) |

---

## После индекса

| VU | Avg, ms | P95, ms | Max, ms | Complete iterations | Throughput, req/s | JSON |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 34.73 | 42.30 | 328.76 | 3436 | 28.63 | [JSON](./k6/results/user-search-1vus-120s-20260327-112017.json) |
| 10 | 21.37 | 94.55 | 544.36 | 55997 | 466.64 | [JSON](./k6/results/user-search-10vus-120s-20260327-112800.json) |
| 100 | 46.95 | 61.60 | 292.95 | 255331 | 2127.76 | [JSON](./k6/results/user-search-100vus-120s-20260327-113309.json) |
| 1000 | 463.35 | 555.27 | 1028.61 | 259314 | 2160.95 | [JSON](./k6/results/user-search-1000vus-120s-20260327-113839.json) |
