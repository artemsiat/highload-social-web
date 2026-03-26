# Dev Services

These helper scripts manage the local Homebrew services used around this project.

Tracked services:

- `postgresql@16`
- `zookeeper`
- `kafka`
- `prometheus`
- `grafana`

Scripts:

- `./scripts/start-dev-services.sh`
- `./scripts/stop-dev-services.sh`
- `./scripts/list-dev-services.sh`

Behavior:

- Start order is `postgresql@16 -> zookeeper -> kafka -> prometheus -> grafana`.
- Stop order is reversed so dependencies are shut down cleanly.
- The scripts use `brew services`, so they manage the same background services you see in `brew services list`.

Examples:

```bash
./scripts/list-dev-services.sh
./scripts/start-dev-services.sh
./scripts/stop-dev-services.sh
```

If you want to track a different set of services later, edit the `SERVICES` arrays in the scripts.
