# Dev Processes

Use this script when you want one combined view of common developer tooling running on your Mac.

Script:

- `./scripts/system/list-dev-processes.sh`

It reports:

- Homebrew services
- project-tracked Homebrew services
- project-tracked non-Homebrew apps
- common developer processes such as IDEs, Docker, Java, Node, Kafka-related tools
- listening local TCP ports

Example:

```bash
bash ./scripts/system/list-dev-processes.sh
```

Notes:

- Some macOS environments restrict process listing, so the script may show partial results.
- Even when process listing is restricted, the Homebrew and Docker checks are still useful.
