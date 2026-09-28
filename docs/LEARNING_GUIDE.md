# Learning guide

## Start with the data

Read migrations V1 through V5 in order. Explain why movie uses the external TMDB ID, while user and playlist use generated IDs. In playlist_movie, the composite primary key prevents the same movie appearing twice in one list, while the separate unique position constraint protects ordering. In user_rating, (user_id, tmdb_id) is the one-rating rule. Deleting a playlist changes a flag rather than destroying its memberships.

## Trace one search

discover.js → GET /api/movies/search → MovieController → MovieService → TmdbClient → TMDB.

The browser never sees a bearer token. MovieService caches and validates external data, then returns a small DTO. Error mapping prevents upstream bodies/credentials from reaching the browser. Read TmdbClientTest to see URL encoding and failures without a real network call.

## Trace one rating edit

cinema-movies.js sends PUT /api/ratings/{id}?userId=... with JSON stars and the CSRF header supplied by ui-api.js. Spring Security requires a session and valid token. OwnershipConfiguration checks that userId matches the session. RatingController checks that the number is an exact integer. RatingService checks bounds/user/movie. RatingRepository performs an upsert in a transaction.

Ask: why is the TMDB check before the database transaction? To avoid holding a database connection while waiting for a remote service. Why does editing not add a vote? The primary key and upsert replace the same row.

## Derive the combined score

Read CombinedScoreCalculator before any framework code. Inputs are external average/count and local sum/count. Work out 8.9×12 + 6×100 divided by 112, then add three local tens and three votes. Compare to 8.4×30000 + 6×100 divided by 30100. Explain why the prior is not displayed as 100 real votes and why null is different from 0.

ScoreService obtains cached external data and fresh local totals, then calls this pure function. ComparisonService and GameService use ScoreService; neither duplicates the formula.

## Trace login and ownership

GET /api/auth/session returns the current user (or null) and a CSRF token. The browser sends that token with POST /api/auth/login. AuthenticationManager asks AccountService for a stored bcrypt hash and checks the password. AuthController invalidates the old session, creates a new one and saves the authenticated security context. The browser fetches a new CSRF token afterward.

Authentication answers “who is this request from?”. Ownership checks answer “does that account own this playlist?”. A client-supplied userId is not proof of identity. Explain why public registration cannot replace ana's null password hash.

## Trace a game round

GameService queries IDs from active playlists, gets shared scores, filters low-vote movies, creates pairs with a >=0.5 gap and snapshots their scores. It returns cards and an opaque round ID, without the scores. On an answer, it verifies the user/run/token/choice, computes correctness itself and saves an improved highscore before advancing. Replaying the previous token fails.

Explain which data survives a restart (database rows) and which does not (sessions, caches, unfinished games). Explain why a game ends successfully if there are no unused eligible pairs left.

## Transactions and edge cases

Read the importer: validation, checksum, advisory lock, writes, ledger entry. All commit together. Then read reorder: lock playlist, compare exact ID sets, delete positions and recreate in one transaction. A partial reorder must never become visible.

Read the integration tests as executable examples: other-user operations fail, duplicate ratings remain one row, seed import preserves edits, null/fractional scores fail, and game replays cannot inflate highscores.

## Self-check before submission

- Explain the controller/service/repository boundary using an actual endpoint.
- Show where each invariant is enforced in Java and/or PostgreSQL.
- Explain the difference between TMDB unavailable and genuinely unrated.
- Explain why same-title remakes use different IDs.
- Change one scoring constant and predict which tests will fail.
- Identify the limitation of in-memory sessions and why it is acceptable locally.
- Explain all AI_LOG entries honestly, including which issues you personally identified.
