# Dev Services

These helper scripts manage the local Homebrew services used around this project.

Tracked services:

- `postgresql@16`
- `zookeeper`
- `kafka`
- `prometheus`
- `grafana`

Scripts:

- `./scripts/brew/start-dev-services.sh`
- `./scripts/brew/stop-dev-services.sh`
- `./scripts/brew/list-dev-services.sh`

Separate scripts for desktop apps that are not managed by Homebrew are documented in [non-brew-apps.md](/Users/artemsiat/IdeaProjects/highload-social-web/docs/non-brew-apps.md).

For one combined machine overview, see [dev-processes.md](/Users/artemsiat/IdeaProjects/highload-social-web/docs/dev-processes.md).

Behavior:

- Start order is `postgresql@16 -> zookeeper -> kafka -> prometheus -> grafana`.
- Stop order is reversed so dependencies are shut down cleanly.
- The scripts use `brew services`, so they manage the same background services you see in `brew services list`.

Examples:

```bash
bash ./scripts/brew/list-dev-services.sh
bash ./scripts/brew/start-dev-services.sh
bash ./scripts/brew/stop-dev-services.sh
```

If you want to track a different set of services later, edit the `SERVICES` arrays in the scripts.
