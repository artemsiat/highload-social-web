# План: кворумная синхронная репликация PostgreSQL

## Цель

Настроить PostgreSQL так, чтобы write-транзакция считалась успешно закоммиченной только после подтверждения хотя бы одной репликой.

Текущая схема:

- `primary` на порту `5434`;
- `replica1` на порту `5435`;
- `replica2` на порту `5436`;
- приложение запускается локально из IDEA;
- база поднимается через `compose-replication.yaml`.

Нужный результат:

```text
primary + replica1 + replica2
commit подтверждается primary только после подтверждения от любой одной replica
```

В PostgreSQL это называется quorum synchronous replication.

---

## Простая теория

Сейчас у нас уже есть streaming replication:

```text
primary -> replica1
primary -> replica2
```

Но она асинхронная:

```text
client -> primary -> commit OK
                  -> WAL потом доезжает до replicas
```

Это быстро, но при падении primary самые последние подтвержденные клиенту транзакции теоретически могут не успеть попасть на реплику.

Синхронная кворумная репликация меняет порядок:

```text
client -> primary -> WAL отправлен хотя бы на 1 replica -> commit OK
```

То есть primary не отвечает клиенту `commit OK`, пока хотя бы одна реплика не подтвердила запись WAL.

---

## Что нужно настроить

Нам нужны две вещи:

1. Реплики должны подключаться к primary с понятными именами:
   - `replica1`
   - `replica2`
2. Primary должен ждать подтверждение от любой одной из этих реплик:

```conf
synchronous_standby_names = 'ANY 1 (replica1, replica2)'
synchronous_commit = on
```

---

## Почему сначала нужно назвать реплики

Сейчас primary видит реплики примерно так:

```text
application_name | state     | sync_state
-----------------+-----------+-----------
walreceiver      | streaming | async
walreceiver      | streaming | async
```

Для кворумной синхронной репликации это неудобно, потому что primary должен понимать, кого именно ждать.

Нам нужно добиться такого состояния:

```text
application_name | state     | sync_state
-----------------+-----------+-----------
replica1         | streaming | async
replica2         | streaming | async
```

И только после этого включать:

```conf
synchronous_standby_names = 'ANY 1 (replica1, replica2)'
```

Если включить `synchronous_standby_names` раньше, write-запросы могут зависнуть: primary будет ждать `replica1` или `replica2`, но фактически будет видеть только `walreceiver`.

Важно: не стоит добавлять `synchronous_standby_names` прямо в `compose-replication.yaml` до первого запуска свежего кластера. При fresh start PostgreSQL должен сначала создать базу `social`, а это тоже write-операция. Если synchronous replication включена сразу, создание базы может зависнуть, потому что реплики еще не подключены.

---

## Шаг 1. Добавить имя реплики в Docker Compose

В `compose-replication.yaml` для `replica1` добавить переменную:

```yaml
REPLICA_NAME: replica1
```

Для `replica2` добавить:

```yaml
REPLICA_NAME: replica2
```

Идея: один и тот же `entrypoint-replica.sh` будет понимать, какую реплику он сейчас запускает.

Ожидаемый смысл:

```yaml
replica1:
  environment:
    REPLICA_NAME: replica1

replica2:
  environment:
    REPLICA_NAME: replica2
```

---

## Шаг 2. Передать `application_name` в `pg_basebackup`

Сейчас реплика создается через `pg_basebackup -R` в файле:

```text
docker/replication/entrypoint-replica.sh
```

`pg_basebackup -R` сам создает файл настроек для реплики и прописывает, как подключаться к primary.

Нужно сделать так, чтобы подключение к primary содержало:

```text
application_name=replica1
```

или:

```text
application_name=replica2
```

Концептуально подключение replica1 должно выглядеть так:

```text
host=primary port=5432 user=replicator password=replicator_pass application_name=replica1
```

А replica2:

```text
host=primary port=5432 user=replicator password=replicator_pass application_name=replica2
```

Практически это можно сделать двумя способами:

- передать `application_name` через connection string в `pg_basebackup`;
- или после `pg_basebackup -R` дописать корректный `primary_conninfo`.

Для учебного проекта проще использовать переменную `REPLICA_NAME` и сделать так, чтобы `primary_conninfo` содержал этот `application_name`.

---

## Шаг 3. Пересоздать реплики

Так как `primary_conninfo` пишется в data directory реплики при первом создании, проще всего пересоздать volumes.

Команда:

```bash
docker compose -f compose-replication.yaml down -v
docker compose -f compose-replication.yaml up -d
```

Важно: `down -v` удалит данные в Docker volumes этой replication-схемы.

После этого приложение снова сможет создать данные через startup generation или Flyway + генерацию тестовых данных.

---

## Шаг 4. Проверить имена реплик

На primary выполнить:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SELECT application_name, state, sync_state FROM pg_stat_replication ORDER BY application_name;"
```

Ожидаемый результат на этом этапе:

```text
application_name | state     | sync_state
-----------------+-----------+-----------
replica1         | streaming | async
replica2         | streaming | async
```

Если все еще видно `walreceiver`, значит `application_name` не применился. В таком случае не переходить к следующему шагу.

---

## Шаг 5. Включить quorum synchronous replication на primary

После того как fresh cluster уже поднялся, база `social` создана, а primary видит `replica1` и `replica2`, включить quorum synchronous replication через SQL:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "ALTER SYSTEM SET synchronous_standby_names = 'ANY 1 (replica1, replica2)';"
```

Затем явно зафиксировать режим commit:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "ALTER SYSTEM SET synchronous_commit = 'on';"
```

Применить конфигурацию:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SELECT pg_reload_conf();"
```

Что это значит:

- `ANY 1` — ждать любую одну реплику;
- `(replica1, replica2)` — список реплик, которые могут подтвердить commit;
- `synchronous_commit=on` — commit считается успешным после flush WAL на primary и подтверждения от synchronous standby.

---

## Шаг 6. Не перезапускать fresh cluster с sync-настройкой в compose

Для текущего учебного setup лучше не держать `synchronous_standby_names` в `compose-replication.yaml`.

Правильный порядок:

1. поднять fresh cluster без synchronous replication;
2. дождаться `replica1` и `replica2`;
3. проверить `pg_stat_replication`;
4. включить quorum synchronous replication через `ALTER SYSTEM`;
5. сделать failover-эксперимент.

---

## Шаг 7. Проверить настройки primary

Проверить значения параметров:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SHOW synchronous_standby_names;"
```

Ожидаемо:

```text
ANY 1 (replica1, replica2)
```

Проверить `synchronous_commit`:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SHOW synchronous_commit;"
```

Ожидаемо:

```text
on
```

---

## Шаг 8. Проверить `sync_state`

На primary выполнить:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SELECT application_name, state, sync_state, sent_lsn, write_lsn, flush_lsn, replay_lsn FROM pg_stat_replication ORDER BY application_name;"
```

Ожидаемый результат:

```text
application_name | state     | sync_state
-----------------+-----------+-----------
replica1         | streaming | quorum
replica2         | streaming | quorum
```

Главное:

```text
sync_state = quorum
```

Это означает, что реплики участвуют в quorum synchronous replication.

---

## Шаг 9. Проверить поведение при остановке одной реплики

Остановить одну реплику:

```bash
docker compose -f compose-replication.yaml stop replica1
```

Попробовать выполнить write-запрос в приложение, например регистрацию пользователя.

Ожидаемо:

- write должен успешно пройти;
- потому что `ANY 1` все еще может получить подтверждение от `replica2`.

Проверить на primary:

```bash
docker compose -f compose-replication.yaml exec -T primary \
  psql -U social_user -d social \
  -c "SELECT application_name, state, sync_state FROM pg_stat_replication ORDER BY application_name;"
```

Ожидаемо:

- будет видна только оставшаяся активная реплика;
- write продолжает работать.

Вернуть реплику:

```bash
docker compose -f compose-replication.yaml start replica1
```

---

## Шаг 10. Понять важный trade-off

Если остановить обе реплики:

```bash
docker compose -f compose-replication.yaml stop replica1 replica2
```

write-запросы могут зависнуть.

Это нормальное поведение synchronous replication:

```text
primary не может подтвердить commit, потому что нет ни одной replica для quorum
```

Вернуть реплики:

```bash
docker compose -f compose-replication.yaml start replica1 replica2
```

---

## Что добавить в отчет

После настройки добавить в отчет:

1. Конфигурацию:

```text
synchronous_standby_names = ANY 1 (replica1, replica2)
synchronous_commit = on
```

2. Скриншот или SQL-вывод:

```sql
SHOW synchronous_standby_names;
SHOW synchronous_commit;
```

3. SQL-вывод:

```sql
SELECT application_name, state, sync_state
FROM pg_stat_replication
ORDER BY application_name;
```

4. Короткий вывод:

```text
Обе реплики подключены в режиме streaming, sync_state = quorum.
Primary подтверждает commit только после подтверждения от любой одной реплики.
```

---

## Patroni

Patroni использовать необязательно.

Для текущего ДЗ лучше не добавлять Patroni, потому что он сильно усложнит setup:

- нужен distributed consensus store, например etcd или Consul;
- появляется автоматическое leader election;
- нужно отдельно настраивать failover;
- становится больше движущихся частей.

Для зачета достаточно:

- настроить streaming replication;
- включить quorum synchronous replication;
- вручную остановить primary;
- вручную промоутить самую свежую реплику;
- проверить, потерялись ли подтвержденные write-транзакции.

---

## Критерий готовности

Этап можно считать готовым, если:

- primary видит `replica1` и `replica2` по именам;
- `SHOW synchronous_standby_names` возвращает `ANY 1 (replica1, replica2)`;
- `SHOW synchronous_commit` возвращает `on`;
- `pg_stat_replication.sync_state` показывает `quorum`;
- write-запрос проходит при одной работающей реплике;
- write-запрос блокируется или не завершается, если обе реплики недоступны.
