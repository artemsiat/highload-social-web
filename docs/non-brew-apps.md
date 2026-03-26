# Non-Brew Apps

These helper scripts manage desktop apps that are not controlled by `brew services`.

Currently tracked:

- `Docker` (Docker Desktop on macOS)

Scripts:

- `./scripts/start-non-brew-apps.sh`
- `./scripts/stop-non-brew-apps.sh`
- `./scripts/list-non-brew-apps.sh`

Examples:

```bash
bash ./scripts/list-non-brew-apps.sh
bash ./scripts/start-non-brew-apps.sh
bash ./scripts/stop-non-brew-apps.sh
```

Notes:

- These scripts are macOS-specific because they use `open` and `osascript`.
- Docker Desktop can take a little time to become ready after launch.
- Use `docker info` to verify the Docker engine is available.
