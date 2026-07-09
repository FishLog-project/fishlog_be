# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`fishlog_be` is the backend for the FishLog project: a Spring Boot 4.1.0 REST API on Java 21, using Spring Web MVC, Spring Data JPA, and MySQL. The codebase is at an early stage — only a health-check endpoint exists so far.

## Config lives in a git submodule

`src/main/resources` is **not** in this repo — it is a git submodule pointing to the private `FishLog-project/be_config` repo (see `.gitmodules`). The real application properties (`application-local.properties`, `application-prod.properties`, and any datasource credentials) live there.

- After cloning, you must run `git submodule update --init --recursive` or the app will not build/run (no resources).
- To pull config changes: `git submodule update --remote`.
- Do not create `application*.properties` in the main repo tree expecting them to be tracked here — they belong to the submodule.

## Commands

```bash
./gradlew build            # full build incl. tests
./gradlew build -x test    # build without tests (this is what CI/CD actually runs)
./gradlew test             # run all tests
./gradlew test --tests 'com.fishlog.fishlog_be.FishlogBeApplicationTests'   # single test class
./gradlew bootRun          # run the app locally (defaults to port 8080)
./gradlew bootRun --args='--spring.profiles.active=local'   # run with the local profile
```

Note: despite inline comments in the workflow files claiming tests are included, both CI and Deploy run `./gradlew build -x test`, so **tests are skipped in the pipeline**. Run them locally before pushing.

## Test configuration

`src/test/resources/application.yml` excludes `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration`, so tests run without a database. Tests that need JPA/MySQL must re-enable those autoconfigurations or provide their own datasource.

## CI/CD

- `.github/workflows/ci.yml` — builds (no tests) on PRs to `main` or `dev`.
- `.github/workflows/deploy.yml` — on push to `main`: builds, pushes a Docker image (`<dockerhub-user>/fishlog-app`, tagged `latest` and the commit SHA) to Docker Hub, then SSHes to EC2 and runs `docker compose pull api && docker compose up -d api`.
- The `Dockerfile` copies `build/libs/fishlog_be-0.0.1-SNAPSHOT.jar` — if `version` in `build.gradle` changes, update the Dockerfile's COPY path to match.

## Branches

`dev` is the default working branch; `main` is the release/deploy branch. Merging to `main` triggers a production deploy.

## Package layout

Root package `com.fishlog.fishlog_be`. Controllers live under `controller/` and are mapped under the `/api` base path (e.g. `GET /api/health`). API docs are available via springdoc-openapi (Swagger UI at `/swagger-ui.html`).