# Contributing to e-CODEX Connector NG

Thank you for your interest in contributing. This page summarizes what a change needs to pass the CI checks.

## Prerequisites

- JDK 21
- Docker, for the integration tests (Testcontainers) and the local Compose stack
- Internet access for the first Gradle build

## Build and test

```shell
# compile, run the unit tests and build the distribution
./gradlew build -x integrationTest

# run the integration tests (requires Docker)
./gradlew integrationTest
```

CI runs `./gradlew test`, `./gradlew integrationTest` and a full `build` (see `.github/workflows/ci.yaml`).

## Code style

Java code is checked with Checkstyle (`config/checkstyle/checkstyle.xml`), also in CI (`linting.yaml`).
Run it locally before you push:

```shell
./gradlew checkstyleMain checkstyleTest
```

## Commit messages

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) and are checked on every push and pull request (`commitlint.yaml`), for example:

```text
fix: correct post-download processing
docs: add a complete documentation entry point
ci: build documentation and upload it as an artifact
```

Use `feat:` for new functionality, `fix:` for bug fixes, `docs:`, `ci:`, `build:`, `refactor:`, `test:` and `chore:` as appropriate.
Mark breaking changes with `!` after the type (`feat!:`) or a `BREAKING CHANGE:` footer.

## Documentation

The documentation lives in `connector-documentation/src/docs/asciidoc` and is built together with the project.
The CI job `docs.yaml` runs when a change touches `connector-documentation/**`, `CHANGELOG.md` or the sample configuration, and uploads the generated HTML and PDF as an artifact.

To build it locally:

```shell
./gradlew :connector-documentation:build
```

The output is in `connector-documentation/build/docs/asciidoc`.

When you change a configuration property, update the sample `application.properties` and the matching configuration chapter.
When you make a user-visible change, add it to **both** `CHANGELOG.md` and `connector-documentation/src/docs/asciidoc/changelog/changelog-7.x.x.adoc`; they are maintained by hand and must stay in sync.

## Pull requests

1. Branch from `develop` and keep the change focused.
2. Make sure the build, the tests, Checkstyle and the commit lint pass.
3. Describe what changed and why in the pull request.
