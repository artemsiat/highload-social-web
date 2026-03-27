# k6

## Install on macOS

The official k6 docs support Homebrew on macOS:

```bash
brew install k6
```

Upgrade later with:

```bash
brew upgrade k6
```

## Should you use Homebrew?

For this project on macOS: yes.

Why:

- simplest install path
- easy upgrades
- no Docker requirement
- good fit for local homework runs

k6 is a CLI tool, not a background service, so it is **not** managed by `brew services`.

## Beginner workflow

1. Start your application.
2. Make sure `/user/search` responds manually first.
3. Run a small k6 test.
4. Increase concurrency to the homework values: `1`, `10`, `100`, `1000`.
5. Save the generated JSON summaries from `docs/homework/2 - оптимизация запросов/k6/results/`.

## Repo-local helper

This homework folder includes:

- `./user-search.js`
- `./run-user-search.sh`

Examples:

```bash
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh'
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 1 30s
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 10 1m
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 100 2m
```

```bash
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh'
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 1
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 10
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 100
bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 1000
```

Optional environment variables:

```bash
FIRST_NAME=Ив LAST_NAME=Пе BASE_URL=http://localhost:8075 bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 10 1m
```

## Multiple search pairs

By default, the runner now uses:

- `./user-search-data.json`

The k6 script cycles through that dataset instead of hammering only one prefix pair.

If you want a custom dataset file:

```bash
SEARCH_DATA_FILE=./user-search-data.json bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 10 1m
```

If you want to force a single pair for a focused benchmark:

```bash
FIRST_NAME=Ив LAST_NAME=Пе bash './docs/homework/2 - оптимизация запросов/k6/run-user-search.sh' 10 1m
```

## What the test checks

- request succeeds with HTTP `200`
- response content type looks like JSON
- k6 prints request summary metrics
- k6 writes a JSON summary file into `./results/`

## Homework usage

Suggested sequence:

1. generate data
2. run k6 before adding the index
3. save latency/throughput results
4. add the index
5. rerun the exact same k6 commands
6. compare before/after
