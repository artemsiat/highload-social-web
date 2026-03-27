# Отчет по ДЗ 2
## Производительность индексов

## 1. Исходные данные

### Запрос

```sql
SELECT *
FROM highload_social_web.users
WHERE first_name LIKE 'Ив%'
  AND last_name LIKE 'Пе%'
ORDER BY id;
```

### Объем данных
- Объем данных: `1 000 000` анкет
- Нагрузочное тестирование: `k6`
- Режим k6: dataset mode (`12` пар префиксов)

### Подробные результаты k6

[Подробный отчет k6](./2-k6-results.md)

---

## 2. Нагрузочное тестирование до индекса

### Скриншоты

#### 1 VU
![1 VU before index](./screens/1%20-%20before%20index.png)

#### 10 VU
![10 VU before index](./screens/10%20-%20before%20index.png)

#### 100 VU
![100 VU before index](./screens/100%20-%20before%20index.png)

#### 1000 VU
![1000 VU before index](./screens/1000%20-%20before%20index.png)

### Краткие результаты до индекса

| Конкурентность | Avg latency, ms | P95 latency, ms | Max latency, ms | Throughput, req/s |
|---|---:|---:|---:|---:|
| 1 | 34.06 | 41.90 | 116.92 | 29.20 |
| 10 | 153.66 | 316.17 | 661.86 | 65.04 |
| 100 | 1373.04 | 1639.65 | 2949.23 | 73.28 |
| 1000 | 12463.25 | 13332.37 | 15856.91 | 84.55 |

---

## 3. Индекс

### SQL добавления индекса

```sql
CREATE INDEX idx_users_search_name_prefix_id
ON highload_social_web.users (
  last_name varchar_pattern_ops,
  first_name varchar_pattern_ops,
  id
);
```

### Почему выбран именно такой индекс

- запрос фильтрует строки по двум префиксным условиям: `last_name LIKE '...%'` и `first_name LIKE '...%'`, поэтому индекс построен именно по этим столбцам;
- `last_name` поставлен первым, потому что в многоколоночном B-Tree наиболее важна левая часть индекса: именно она сильнее всего влияет на сужение диапазона поиска при prefix-условиях;
- `first_name` поставлен вторым, чтобы второе условие тоже участвовало в индексном поиске и уменьшало число строк, которые нужно читать из heap;
- для обоих `varchar`-столбцов использован `varchar_pattern_ops`, потому что запрос использует prefix `LIKE '...%'`, и именно этот operator class позволяет PostgreSQL уверенно использовать B-Tree индекс для такого сравнения;
- `id` добавлен последним, потому что результат сортируется по `id`, то есть индекс соответствует не только фильтрации, но и структуре итогового запроса;
- фактический `EXPLAIN (ANALYZE, BUFFERS)` подтвердил использование этого индекса: PostgreSQL выполнил `Bitmap Index Scan` по `idx_users_search_name_prefix_id`, а в `Index Cond` попали оба prefix-условия.

---

## 4. EXPLAIN после индекса

### Команда

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM highload_social_web.users
WHERE first_name LIKE 'Ив%'
  AND last_name LIKE 'Пе%'
ORDER BY id;
```

### Результат

```text
Sort  (cost=802.27..802.52 rows=99 width=192) (actual time=1.455..1.459 rows=65 loops=1)
  Sort Key: id
  Sort Method: quicksort  Memory: 38kB
  Buffers: shared hit=138
  ->  Bitmap Heap Scan on users  (cost=430.29..798.99 rows=99 width=192) (actual time=1.359..1.428 rows=65 loops=1)
        Filter: (((first_name)::text ~~ 'Ив%'::text) AND ((last_name)::text ~~ 'Пе%'::text))
        Heap Blocks: exact=65
        Buffers: shared hit=138
        ->  Bitmap Index Scan on idx_users_search_name_prefix_id  (cost=0.00..430.27 rows=96 width=0) (actual time=1.344..1.344 rows=65 loops=1)
              Index Cond: (((last_name)::text ~>=~ 'Пе'::text) AND ((last_name)::text ~<~ 'Пж'::text) AND ((first_name)::text ~>=~ 'Ив'::text) AND ((first_name)::text ~<~ 'Иг'::text))
              Buffers: shared hit=73
Planning:
  Buffers: shared hit=42
Planning Time: 0.282 ms
Execution Time: 1.496 ms
```

### Скриншот EXPLAIN
![EXPLAIN after index](./screens/explain%20after%20index.png)

### Краткий вывод по плану

- PostgreSQL использует `Bitmap Index Scan` по `idx_users_search_name_prefix_id`;
- обе prefix-условия участвуют в `Index Cond`;
- после выборки строк остается `Sort` по `id`;
- итоговое время выполнения запроса: `1.496 ms`.

---

## 5. Нагрузочное тестирование после индекса

### Скриншоты

#### 1 VU
![1 VU after index](./screens/1%20-%20after%20index.png)

#### 10 VU
![10 VU after index](./screens/10%20-%20after%20index.png)

#### 100 VU
![100 VU after index](./screens/100%20-%20after%20index.png)

#### 1000 VU
![1000 VU after index](./screens/1000%20-%20after%20index.png)

### Краткие результаты после индекса

| Конкурентность | Avg latency, ms | P95 latency, ms | Max latency, ms | Throughput, req/s |
|---|---:|---:|---:|---:|
| 1 | 34.73 | 42.30 | 328.76 | 28.63 |
| 10 | 21.37 | 94.55 | 544.36 | 466.64 |
| 100 | 46.95 | 61.60 | 292.95 | 2127.76 |
| 1000 | 463.35 | 555.27 | 1028.61 | 2160.95 |

---

## 6. Итог

### Сравнение до / после

- на `10 VU` throughput вырос примерно с `65 req/s` до `467 req/s`;
- на `100 VU` throughput вырос примерно с `73 req/s` до `2128 req/s`;
- на `1000 VU` throughput вырос примерно с `85 req/s` до `2161 req/s`;
- latency после индекса заметно снизилась на `10`, `100` и `1000` VU;
- при `1000 VU` P95 все еще выше порога `500 ms`, но результат стал значительно лучше, чем до индекса.

### Вывод

Добавление индекса `idx_users_search_name_prefix_id` существенно ускорило поиск `/user/search` под нагрузкой. План выполнения подтверждает использование индекса, а результаты k6 показывают заметное снижение latency и сильный рост throughput после индекса.
