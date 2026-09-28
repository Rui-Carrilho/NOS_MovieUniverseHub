# Design checkpoint

This records the earlier checkpoint. The current dark, dedicated-page design is documented in [DARK_PAGES.md](DARK_PAGES.md).

## Scope delivered

- Dedicated /login and /register pages with show/hide password, validation and inline errors.
- Server-side access control: unauthenticated requests for the dashboard, workspace and API reference redirect to /login. Movie APIs now require authentication. An authenticated visitor to /login or /register redirects home.
- A warm off-white dashboard, dark green navigation, restrained orange actions, editorial typography and poster-led playlist/movie cards.
- Real profile name, playlist counts, membership counts and a five-movie preview from the user's collection. No invented recommendations or recent-activity claims.
- Create-playlist dialog, empty states, unavailable movie/poster states, logout and responsive layouts.
- Playlist and movie cards deep-link into the existing authenticated workspace.
- The game has its own authenticated page and sidebar tab. A round shows both combined scores after an answer; the next pair appears when the player chooses to continue. No game requests run on initial dashboard load.

## Deliberately pending the design review

Discovery, full playlist pages, movie details and comparison still use the previous workspace at /workspace.html. They are functional but have not yet received the new full visual treatment. This checkpoint establishes the visual direction before spreading it across those flows.

## Files to read

- auth/PageController.java: page routes and authenticated redirects.
- auth/SecurityConfiguration.java: server-side page/API gate.
- static/auth.html and auth-page.js: focused authentication UI.
- static/index.html and dashboard.js: new dashboard and actual library data.
- static/ui-api.js: session/CSRF/request handling for new screens.
- static/checkpoint.css: shared visual rules, responsive states and temporary workspace visibility rules.
- static/workspace.html and app.js: preserved functional flows with deep-link support.
- static/game.html and game.js: standalone game page, round feedback and highscores.

Java files are under src/main/java/com/movieuniverse/hub; static files under src/main/resources.

## Verification

40 Java tests passed, including protected dashboard/workspace/game pages and authenticated movie endpoints. The JavaScript formatting suite and syntax checks also passed.

Browser checks used an isolated local PostgreSQL database and fixture movie responses, with public TMDB poster images for visual review. Verified desktop login/dashboard, login redirect, new-playlist creation and movie-card detail navigation. Reviewed the mobile breakpoint and replaced its scrolling navigation with a visible two-column menu. The actual user database and .env were not accessed.

The game page was checked separately in a browser with disposable API responses: start, correct answer, score reveal, next pair, wrong answer and replay. At a 390px viewport the navigation and two choice cards fit without horizontal overflow. The local fixture server was stopped after the check.

## Review locally

From the project root:

```bash
docker compose up -d --build app
```

Then open http://localhost:8081/login. If already signed in, use the logout icon in the new sidebar to review login. The game is at http://localhost:8081/game.html after signing in. Hard-refresh if your browser retained old styling.

Review the login composition, colour/typography, navigation, playlist covers and movie cards. The next pass should apply the agreed direction to the remaining screens, rather than add more features.
