# Dark cinema pages

The UI now has a dedicated page for each sidebar destination. The original combined `workspace.html` is a small redirect for old links; it no longer loads the old all-in-one script. `app.js` and `styles.css` were removed after their features moved to focused modules.

| Page | Script | Main job |
| --- | --- | --- |
| `/` | `dashboard.js` | Overview, playlist tiles, movie preview, create playlist |
| `/discover.html` | `discover.js` | Search, pagination, save/remove in a selected playlist |
| `/playlists.html` | `playlists.js` | Create, rename, delete/restore, order, export and view films |
| `/compare.html` | `compare.js` | Compare averages, coverage and overlap |
| `/game.html` | `game.js` | Play rounds and show local highscores |
| `/login`, `/register` | `auth-page.js` | Authentication |
| `/about.html` | `about.js` | Attribution and scoring explanation |

`cinema.css` applies the charcoal, near-black and red palette to every page, including login, the dashboard, the game and API reference. `checkpoint.css` retains the existing layout rules underneath it. `cinema-shell.js` provides the shared session/logout setup and simple DOM helpers. `cinema-movies.js` owns the movie-detail dialog, combined score and local rating form used by discovery and playlists. `ui-api.js` supplies CSRF headers for every mutation.

The backend still protects each HTML page and all private API routes. An unauthenticated direct URL redirects to `/login`. The three new pages use the existing API services and database; no schema migration is needed. The TMDB token remains on the server.

## Try the flows locally

Run `docker compose up -d --build app` and open `http://localhost:8081/login`.

1. In Discover, search a title, open its details, rate it, select a playlist and save/remove it. Check a second result page if available.
2. In Playlists, create a list, add a film from Discover, rename it, reorder its films, export JSON, delete it and restore it.
3. In Compare, select two distinct lists, inspect average/coverage/overlap, then change a rating and compare again.
4. Check the dashboard, game, login and each of the three new pages at desktop and mobile widths. Directly open a protected page after logout to confirm the redirect.

Some operations require TMDB network access. A missing token, unavailable film or provider error should show a message without hiding the rest of the page.
