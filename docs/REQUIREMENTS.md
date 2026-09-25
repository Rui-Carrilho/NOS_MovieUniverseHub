# Assignment requirement map

“Implemented” describes code present; “verified” is limited to RELATORIO.md. Docker and live TMDB acceptance remain local checks.

| Requirement | Implementation / evidence |
|---|---|
| HTML/CSS/JavaScript web UI and REST backend | Static UI and Spring MVC; browser smoke test |
| TMDB title search and movie details | MovieService/TmdbClient; HTTP fixture tests and browser flow |
| Synopsis, genres, duration and image | Detail DTO/UI; live posters need real-token check |
| TMDB rating with votes; no votes distinct | format.mjs and mapping tests |
| Named playlists, homepage highlights, add/remove stars | Playlist services, JDBC and browser UI |
| Editable 1–10 rating, one per user/movie | Rating upsert and primary key; PostgreSQL tests |
| Seed command, unchanged data, repeatability | Runner/service; byte equality, counts and repeated import verified |
| Deleted lists, duplicates, remakes, deleted-only movies | ID-based identity, soft deletion, first duplicate retained |
| Weighted combined score and low-vote examples | Pure CombinedScoreCalculator; numerical tests |
| No combined score with zero votes | null score plus explanation; tests |
| Playlist means and winner | Coverage, ties, unavailable handling, overlap; tests and browser flow |
| Data/business/presentation layers | Repositories, services, controllers and static UI |
| Local app/database; one-command startup | Dockerfile, Compose and README; Docker execution unverified here |
| Server-only token, ignored .env, blank template | Git/build exclusions and backend configuration; real token not read |
| Feature branch merged into main | feature/final-polish local merge; existing user commit preserved |
| GitHub repository | Existing origin/main observed; final local commits require review/push |
| Explainable code | Learning guide and batch map; candidate must study and explain it |
| AI_LOG with at least three real cases | User corrections and assistant/test findings distinguished honestly |
| Optional secure login | Spring Security, bcrypt, CSRF, session rotation and ownership tests |
| TMDB attribution | About logo/notice and README |
| Optional caching | Bounded search/details/config caches and documented TTLs |
| Optional game and persistent highscores | Shared score, eligibility, one-use rounds; tests and browser fixture checks |
| Valued OpenAPI | /openapi.json and local /api.html reference |
| Submission documents | README, LICENSE, DECISIONS, AI_LOG, RELATORIO and unchanged seed |
| Extras | Rename, restore, reorder, export, comparison overlap and coverage |

Publication and personal understanding cannot be established by automated tests. Run docs/ACCEPTANCE.md before submission.
