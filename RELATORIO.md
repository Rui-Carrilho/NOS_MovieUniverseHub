# Verification report — 25 September 2026

## Executed checks

| Case | Result |
|---|---|
| Maven verify | Passed: 39 Java tests, 0 failures, 0 errors; executable JAR packaged. |
| JavaScript formatting | Passed: 3 tests covering no votes, missing dates and poster-host restrictions. |
| Browser JavaScript syntax | Passed module syntax check. |
| Application/Flyway startup | Passed against an isolated PostgreSQL 14.15 instance; V1–V5 applied. |
| Seed fidelity | Byte-for-byte equality with seed_playlists.json in the supplied ZIP. |
| Fresh seed import | Passed: 3 users, 10 playlists, 34 movie IDs, 50 memberships, 20 ratings; one duplicate skipped. |
| Repeat seed import | Passed: no duplicate inserts, later playlist edits retained. |
| Invalid seed | Rejected without inserting its user. |
| Rating edit | Upsert retained one row while changing stars; fractional HTTP input rejected. |
| Combined-score low-vote rule | Passed: 8.9/12 stays below 8.4/30000, even with three local tens. |
| No votes | Absent combined score, zero real votes; not a misleading zero rating. |
| Comparison | Passed overlap, shared-movie evaluation, tie, empty list and unavailable-data cases. |
| Authentication | Real registration/login/session rotation/logout exercised through Spring Security filters. |
| CSRF | Missing tokens rejected, including registration; real bootstrap/refresh tokens accepted after login. |
| Ownership | Cross-user list reads, exports, membership writes, rating writes and forged create bodies rejected. |
| Seed account protection | Public registration cannot claim an existing passwordless profile. |
| Password storage | Database value is a bcrypt hash, not the supplied password. |
| Playlist reorder/delete/restore | Reordering preserved exact IDs; invalid duplicate order rejected; soft deletion preserved memberships. |
| Game | Correct/wrong answers, low-vote exclusion, score concealment, round replay rejection and persisted highscore passed. |
| API specification | OpenAPI JSON parsed; 24 paths; specification and reference routes accessible. |
| Packaged seed command | Ran and exited successfully; repeated startup reported already imported. |
| Packaged password command | Ran and exited successfully using a disposable fixture account. |
| Browser smoke test | Login, own playlists, search, details, rating edit, comparison, game correct/wrong answers, highscore display and logout passed with local fixture TMDB data. |
| Visual check | Signed-in library and comparison layouts inspected in the in-app browser. |

## Environment and scope

Tests used Java 25 compiling for Java 21, Spring Boot 4.1.1 and isolated PostgreSQL 14.15 from the test-only Zonky library. The application Docker configuration targets PostgreSQL 17. The test database and HTTP fixture server were separate from the user's existing database and credentials. The actual TMDB token was not read. Browser fixtures deliberately used fake dates/scores and absent posters; they do not establish live TMDB correctness or poster availability.

The original contextLoads test was replaced with isolated database/HTTP integration coverage. The final Maven run includes the entire test suite, not just the batch-1 filter.

## Remaining local checks

- Docker image build and docker compose up were not executed successfully in the agent environment because Docker socket access was unavailable. Docker configuration and startup files were prepared; verify this path locally.
- Live TMDB search/details/posters with the user's own token were not exercised. Run docs/ACCEPTANCE.md.
- Browser create/rename/restore/reorder/export controls have automated backend coverage and code review; they were not all manually exercised in the browser smoke test.
- Full mobile, accessibility, load and production-security testing were not performed.
- The final local commits must be reviewed and pushed to the existing GitHub repository.

## Genuine defects found and fixed

The first final-batch integration run failed because contains(null) throws on some immutable Java lists. The reorder null check was corrected and the regression passes. Earlier fixes cover boxed Long identity and fractional rating coercion. See AI_LOG.md; these were not staged as artificial user discoveries.
