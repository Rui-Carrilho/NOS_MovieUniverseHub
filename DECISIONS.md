# Decisions

## Stack and layers

Spring Boot keeps continuity with the developer's recent Java experience. Plain JavaScript avoids a second framework/build tool while satisfying the required HTML/CSS/JS frontend. PostgreSQL provides durable storage, constraints, transactions and advisory locks. JDBC keeps the actual SQL visible and explainable; Flyway tracks additive schema changes.

## Combined score

For TMDB average T, TMDB votes V, local sum S and local count N:
`(T*V + S + 100*6)/(V+N+100)`.

The fixed reference is 6 with weight 100. Those 100 are a prior, never displayed as real votes. If V+N=0, return null, not zero and not the reference value. Local votes influence the same weighted sum as external votes. There is no second average-of-averages step. Display rounding is separate from comparisons.

- 8.9 with 12 votes → 6.310714...
- The same movie plus three local tens → 6.406957...
- 8.4 with 30,000 votes → 8.392027...
- The high-evidence movie remains ahead in both cases.

The choice is intentionally conservative and is application policy, not a statistically learned universal optimum. An unresolved product question is how TMDB vote-count semantics and update frequency should be treated if different sources report inconsistent counts; this version uses the count returned with the same TMDB detail response. No invented votes are added to displayed counts.

## Playlist comparison

Each distinct rated movie contributes equally to the playlist arithmetic mean. Unrated movies are excluded and coverage is shown. Empty/all-unrated playlists cannot win. A failed TMDB lookup is unavailable rather than unrated; any unavailable movie makes the comparison incomplete and suppresses a winner. A provider-wide 5xx failure stops remaining lookups, avoiding repeated timeouts. Ties use a small floating-point tolerance (1e-9). Extra comparisons are Jaccard overlap and common/unique IDs.

## Seed anomalies and persistence

The supplied file is unchanged. First occurrence of duplicate movie membership wins, including its position; different movies cannot occupy the same position. Identity is TMDB ID, never title, so remakes remain separate. Soft-deleted lists and their movies stay in the database. Only active lists appear in the home page and game pool.

Import uses a checksum ledger, a PostgreSQL advisory transaction lock and one transaction for all writes. It is repeatable and preserves later edits. Counts in a clean database are 3 users, 10 lists, 34 movies, 50 memberships and 20 ratings; existing user-created data can increase those counts.

## Authentication and privacy

Spring Security handles session security, CSRF and request authentication; passwords use bcrypt cost 12 with per-password salts. Password input is at least 10 characters and no more than 72 UTF-8 bytes to avoid bcrypt truncation. Login replaces the prior session. Cookies are HttpOnly/SameSite=Strict, with 30-minute session timeout; Secure cookies require an HTTPS deployment and are not enabled for this localhost HTTP setup.

Every supplied userId must match the session. Playlist-path ownership is checked centrally, and playlist creation checks its body owner too. The user API exposes only the current user. Aggregate movie scores are public; individual ratings require the matching authenticated profile. Highscore names are visible to logged-in local users and the UI discloses this.

Seed profiles have null password hashes and cannot log in until the computer's operator uses the local password command. Public registration cannot claim an existing username. Changing a password locally does not revoke all existing sessions; stop/restart the application or log out to clear them.

## Game

Use the same ScoreService and CombinedScoreCalculator as the rest of the app. Consider the first 100 distinct IDs (ascending) across the user's active playlists. Eligibility: at least 100 real total votes; pair difference >= 0.5. A 404 movie is excluded; provider-wide failures abort game creation.

The server snapshots eligible scores, shuffles pairs and issues an opaque one-use round ID. The client never supplies a score or running total. Scores are revealed only after answering; each pair appears at most once per run. A wrong answer ends the run; exhausting pairs ends it successfully. Persist an improved highscore before advancing the round, making a failed database write retryable. One in-memory run per user, up to 1,000 cached runs, expires after 30 idle minutes. Session/run loss on restart is acceptable for this local exercise; highscores persist.

## Additional functionality

Rename, restore, membership reorder, export and an OpenAPI reference were added after the required catalogue/ratings/comparison core. Reordering validates the exact set of current IDs and replaces their positions inside a locked transaction. Export includes only the owning user's selected playlist and movie IDs.

## References

- [Spring Security password storage](https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html)
- [Zonky embedded PostgreSQL test library](https://github.com/zonkyio/embedded-postgres)
- [TMDB developer documentation](https://developer.themoviedb.org/docs)
