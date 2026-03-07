.PHONY: dev db full down reset

dev:
	./scripts/dev.sh up

db:
	./scripts/dev.sh db

full:
	./scripts/dev.sh full

down:
	./scripts/dev.sh down

reset:
	./scripts/dev.sh reset
