# MovieUniverse Hub

Local movie discovery, private playlists, editable ratings, conservative combined scores, playlist comparison and a higher/lower game.

Java 21, Spring Boot 4.1.1, PostgreSQL and plain HTML/CSS/JavaScript. No frontend build step. Docker is the supported all-in-one startup path; IntelliJ + a Docker database also works.

## Start on a clean machine

Prerequisites: Git and Docker Engine/Desktop with the Docker Compose v2 plugin, running and accessible to your account. Internet is needed for the first image/dependency downloads and TMDB requests.

1. Clone the repository and open a terminal in its root (the folder containing pom.xml and compose.yaml).
2. Copy the environment template: `cp .env.example .env`.
3. Edit .env locally. Set POSTGRES_DB, POSTGRES_USER and a strong POSTGRES_PASSWORD. Set TMDB_API_TOKEN to your TMDB **API Read Access Token** (without the word Bearer). Obtain it from your own TMDB account's API settings. Never paste it into chat, commit it, or put it in frontend code.
4. Stop any IntelliJ instance using port 8081. Start everything with:

```bash
docker compose up --build
```

Open http://localhost:8081. Flyway creates/updates the schema automatically. Startup imports the unchanged supplied seed. Repeated imports do not duplicate data or replace edits.

The database remains the existing postgres-data Compose volume; this change does not rename it. The database host port is 5433 and the browser port is 8081, both bound to localhost. If those ports are occupied, stop the previous process or adjust the documented compose ports and your browser URL.

Register a new account in the page, or enable a seeded account using the local command below. Seeded users have **no default password** and cannot be claimed through registration.

## Use the seeded users

With Docker Compose running, in another terminal:

```bash
./scripts/set-password.sh
```

Choose ana, bruno or carla when prompted and set a private password. The script reads the password without echoing it and passes it through the process environment, not the command line or source files. It runs a local administrative command; there is no HTTP password-claim endpoint. Log out existing sessions after a local password change; it is a development administration command, not a full account-recovery system.

Registering a new account does not automatically copy another user's playlists. This preserves ownership.

## Stop / restart

```bash
docker compose stop
docker compose up
```

Do not use `docker compose down -v` unless you deliberately want to delete your database. Code backups do not back up Docker volumes.

## IntelliJ / Maven alternative

Install a JDK 21 or newer; Maven Wrapper downloads Maven. Import pom.xml in IntelliJ. Use the project root as the working directory.

```bash
docker compose up -d db
./mvnw spring-boot:run
```

For a first seed import in this mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --app.seed.enabled=true --app.seed.exit=true"
```

To enable a seeded account without the application container, use a Bash terminal:

```bash
read -r -p "Profile: " HUB_ACCOUNT_USERNAME
read -r -s -p "Password: " HUB_ACCOUNT_PASSWORD
printf '\n'
export HUB_ACCOUNT_USERNAME HUB_ACCOUNT_PASSWORD
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --app.account.enabled=true"
unset HUB_ACCOUNT_USERNAME HUB_ACCOUNT_PASSWORD
```

The .env import supplies local database settings. A missing TMDB token does not prevent startup, but movie requests show a configuration error. The container receives the variables through Compose; .env is excluded from the image.

## Import explicitly in Docker

```bash
docker compose run --rm --no-deps app --spring.main.web-application-type=none --app.seed.enabled=true --app.seed.exit=true
```

Run this after the database is healthy. The supplied JSON is in dados/seed_playlists.json and is unchanged from the supplied ZIP.

Fresh database expectations: 3 users, 10 playlists (8 active, 2 deleted), 34 distinct movie IDs, 50 memberships after removing one repeated membership, and 20 ratings. Deleted-only movies remain stored but are not displayed as active memberships. Imported order is preserved; repeated movie entries keep the first occurrence. Re-running the same file is a no-op, enforced by its checksum and a transaction-level advisory lock.

## Features

- Search by title, paginated results and full movie details.
- TMDB rating always paired with real vote counts; no votes is shown explicitly.
- Create, rename, soft-delete, restore, reorder and export your playlists.
- Stars add/remove movie IDs without duplicate memberships.
- One editable rating (integer 1–10) per user/movie, and rating deletion.
- Combined scores, coverage-aware comparisons and movie overlap.
- Session authentication, bcrypt passwords, CSRF protection and server-side ownership checks.
- Higher/lower game and persisted local highscores.
- Local API reference at /api.html and OpenAPI 3.1 at /openapi.json.

Export downloads a playlist and its ordered movie IDs as JSON. This is a portable view of that playlist, not a full database backup or an automatic re-import format.

## Tests

Install JDK 21+ to run Java tests. Node.js 20+ is only needed for the small optional JavaScript test suite.

```bash
./mvnw test
node --test src/test/js/format.test.mjs
```

Java tests start an isolated embedded PostgreSQL 14.15 instance using Zonky's test-only library. They override the .env import, use their own database and mock TMDB. Docker is not required for these tests. First execution downloads native test binaries; run as a normal user, not root. Supported binary platforms are determined by Zonky; Linux x86_64 was verified. The application Compose database is PostgreSQL 17.

See RELATORIO.md for measured results and explicit unverified checks, and docs/ACCEPTANCE.md for the manual workflow.

## Scoring, caching and limitations

Combined score:
`(tmdbAverage * tmdbVotes + localRatingSum + 100 * 6) / (tmdbVotes + localVotes + 100)`.

No real votes means no combined score. The prior does not count as real votes. DECISIONS.md explains the low-vote examples, playlist mean, and game eligibility.

TMDB search responses: 5-minute cache, maximum 200 entries; details: 1 hour, maximum 1,000 entries; image configuration: 24 hours, one entry. Failed calls are not cached. Local ratings and combined scores are read/calculated fresh. All caches and sessions are in memory and clear on restart. Database playlists, ratings and highscores persist.

The game considers up to the first 100 distinct movie IDs from your active playlists, requires 100 real votes and a score difference of at least 0.5. Scores are snapshotted when the game starts. A pair is not repeated in a run. Completed runs can end by exhausting eligible pairs. An unfinished run expires after 30 minutes of inactivity or an application restart; a correct answer's highscore is saved immediately.

This is a local assignment application, not a production hosting configuration. Only localhost ports are published. There is no email collection, account recovery service, distributed session store or production TLS configuration. Registration/login are limited to 10 attempts per remote IP per minute.

## Architecture / learning

Read docs/BATCH_2.md for the new code and docs/LEARNING_GUIDE.md for the end-to-end walkthrough. Layers are controllers → services → JDBC repositories, with TMDB isolated behind its client/service and the scoring policy in a pure Java class.

The learning copy is separate from this project. Its copied .env would connect to the same database: configure a separate database before experimenting. Read its BACKUP_RECOVERY.md for the recovery limitation.

## Attribution and license

Movie metadata and posters are provided by [TMDB](https://www.themoviedb.org/).
This product uses the TMDB API but is not endorsed or certified by TMDB.
The About section displays the TMDB logo and the required attribution.

Application code: MIT (LICENSE). The license does not grant rights to TMDB data/logo or override the supplied assignment data's terms. Dependency licenses remain their own.

## Submission

Review AI_LOG.md honestly, run the manual checks with your own token, and publish the project to your chosen GitHub repository. Do not publish .env, target/, work/, IDE files or database dumps. docs/SUBMISSION.md covers the Git/submission checklist.
