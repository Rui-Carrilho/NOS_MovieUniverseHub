# Final batch: implementation map

This batch is implemented in the main project. Batch 1 (catalogue, UI, ratings, scoring and comparison) was applied first. The separate learning copy was not used as an implementation target.

## New and changed areas

| Area | Files | What to understand |
|---|---|---|
| Passwords and accounts | auth/AccountService, PasswordConfiguration | Validation, salted bcrypt hashes, no public claiming of seed users |
| HTTP security | auth/SecurityConfiguration, AuthController | Session cookie, CSRF bootstrap, login rotation, logout, error responses |
| Ownership | auth/CurrentUser, OwnershipConfiguration; PlaylistController; UserController | Session identity controls access; body/query/path checks have distinct jobs |
| Seed account setup | auth/LocalAccountRunner; scripts/set-password.sh | Local command only, no password reset HTTP endpoint |
| Playlist extras | playlist/PlaylistManagementService and Controller | Rename, deleted lists, transactional reorder, export |
| Game | game/GameService and Controller | Shared scores, eligibility, snapshot, one-use round, durable highscore |
| Database | V5__accounts_and_highscores.sql | Additive migration; V1–V4 unchanged |
| Browser | static/index.html, app.js, styles.css | Login forms, CSRF headers, library management, game |
| API reference | static/openapi.json, api.html, api-docs.js | 3.1 specification, local read-only reference |
| Startup | Dockerfile, compose.yaml, .dockerignore | Build application, wait for DB, import seed, protect local secrets |
| Verification | MovieUniverseHubApplicationTests | Real isolated PostgreSQL, actual HTTP security filters, mocked TMDB |

Java package paths above are under src/main/java/com/movieuniverse/hub. Static files are under src/main/resources. The learning guide gives concrete request traces.

## Compatibility

No existing migration was rewritten. Password_hash is nullable so the supplied seed stays unchanged. Existing accounts gain password login only when enabled locally. All previous name-selector clients must now authenticate before reading or writing private user data.

The existing PostgreSQL Docker volume is preserved by keeping the same Compose service/volume names. The Compose app binds inside its container to 0.0.0.0, while publishing only localhost on the host. Local IntelliJ execution still binds to 127.0.0.1.

## What to do next

1. Read README.md startup instructions; run Docker Compose yourself.
2. Set a password for a seed profile or register a new account.
3. Follow docs/ACCEPTANCE.md with your own TMDB credentials.
4. Read LEARNING_GUIDE.md, especially the score and ownership traces.
5. Review AI_LOG.md and publish your chosen GitHub repository after verifying secrets are excluded.

The final report lists exactly which checks the assistant actually executed. Docker/live TMDB checks must not be described as passed unless they were run successfully.
