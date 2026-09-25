# Local acceptance checklist

Run these against your local application with your own token. Do not reset or delete your existing volume to test.

- [ ] Start with docker compose up --build; verify database becomes healthy and app serves localhost:8081.
- [ ] Enable ana locally, log in, and see only her active playlists.
- [ ] Log out and verify private library requests require login.
- [ ] Register a different account; its library starts empty. Registering ana again must fail.
- [ ] Search Inception; inspect synopsis, genre, runtime, poster, TMDB score and vote count.
- [ ] Search a title with remakes; verify different years/IDs stay distinct.
- [ ] Create a playlist, star a movie from results, reload, then remove it.
- [ ] Rate a movie 8, change to 10, and confirm local count does not increase on edit.
- [ ] Check combined score beside TMDB score and the displayed real vote count.
- [ ] Rename and reorder a playlist; reload to confirm persistence.
- [ ] Soft-delete a playlist, restore it, and confirm the memberships survived.
- [ ] Export a playlist and inspect the JSON; it must contain only that playlist.
- [ ] Compare two playlists, then an empty list; verify coverage and no false winner.
- [ ] Play the game; test a correct answer, wrong answer and a new run.
- [ ] Restart the app; verify playlists, ratings and highscores remain, while login/game session may reset.
- [ ] Run the seed import command twice; later edits must remain.
- [ ] Open /api.html and /openapi.json.
- [ ] Inspect the browser Network panel: no real TMDB token should be present.
- [ ] Review AI_LOG.md and complete the GitHub submission checklist.
