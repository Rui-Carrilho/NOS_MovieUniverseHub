package com.movieuniverse.hub;

import com.movieuniverse.hub.auth.*;
import com.movieuniverse.hub.game.GameService;
import com.movieuniverse.hub.movie.*;
import com.movieuniverse.hub.playlist.*;
import com.movieuniverse.hub.rating.*;
import com.movieuniverse.hub.seed.SeedImportService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.import=", "app.seed.enabled=false", "tmdb.api-token=", "app.account.enabled=false"})
@AutoConfigureMockMvc
class MovieUniverseHubApplicationTests {
    static final EmbeddedPostgres database;
    static {
        try { database = EmbeddedPostgres.builder().setPort(0).start(); }
        catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> database.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired PlaylistRepository playlists;
    @Autowired PlaylistMovieService memberships;
    @Autowired PlaylistManagementService management;
    @Autowired RatingService ratings;
    @Autowired SeedImportService importer;
    @Autowired GameService game;
    @MockitoBean MovieService movies;
    long alice, bob, playlist;

    @BeforeEach void prepare() {
        jdbc.execute("TRUNCATE game_highscore, user_rating, playlist_movie, playlist, movie, app_user, seed_import RESTART IDENTITY CASCADE");
        alice = jdbc.queryForObject("INSERT INTO app_user(username) VALUES ('alice') RETURNING id", Long.class);
        bob = jdbc.queryForObject("INSERT INTO app_user(username) VALUES ('bob') RETURNING id", Long.class);
        playlist = playlists.create("Alice's list", alice).id();
        when(movies.details(anyLong())).thenAnswer(call -> movie(call.getArgument(0), 8.0, 1000));
    }
    static MovieDetails movie(long id, double score, long votes) {
        return new MovieDetails(id, "Movie " + id, "2020-01-01", "Overview", List.of("Drama"), 100, null, score, votes);
    }
    @Test void seedImportsTwiceAndPreservesChanges() throws Exception {
        jdbc.execute("TRUNCATE game_highscore, user_rating, playlist_movie, playlist, movie, app_user, seed_import RESTART IDENTITY CASCADE");
        byte[] bytes = Files.readAllBytes(Path.of("dados/seed_playlists.json"));
        var first = importer.importBytes(bytes);
        assertThat(first.usersInserted()).isEqualTo(3);
        assertThat(first.playlistsInserted()).isEqualTo(10);
        assertThat(first.moviesInserted()).isEqualTo(34);
        assertThat(first.membershipsInserted()).isEqualTo(50);
        assertThat(first.ratingsInserted()).isEqualTo(20);
        assertThat(first.duplicateMembershipsSkipped()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM playlist WHERE NOT deleted", Integer.class)).isEqualTo(8);
        jdbc.update("UPDATE playlist SET name = 'Edited' WHERE source_id = 'pl-01'");
        assertThat(importer.importBytes(bytes).alreadyImported()).isTrue();
        assertThat(jdbc.queryForObject("SELECT name FROM playlist WHERE source_id = 'pl-01'", String.class)).isEqualTo("Edited");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM playlist_movie", Integer.class)).isEqualTo(50);
    }
    @Test void invalidSeedDoesNotWriteAnything() {
        assertThatThrownBy(() -> importer.importBytes("""
                {"versao":"1.0","utilizadores":[{"nome":"intruder"}],"playlists":[],
                 "notas":[{"utilizador":"intruder","tmdb_id":1,"estrelas":11,"data":"2026-09-01"}]}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE username='intruder'", Integer.class)).isZero();
    }
    @Test void anonymousAndCrossAccountRequestsCannotAccessPrivateData() throws Exception {
        mvc.perform(get("/api/playlists").param("userId", "" + alice)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/playlists").param("userId", "" + alice).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/playlists/" + playlist).with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/playlists/" + playlist + "/export").with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(put("/api/playlists/" + playlist + "/movies/603").with(user("bob")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/playlists").with(user("bob")).with(csrf()).contentType("application/json")
                .content("{\"name\":\"Forgery\",\"userId\":" + alice + "}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/ratings/603").param("userId", "" + alice).with(user("bob")).with(csrf())
                .contentType("application/json").content("{\"stars\":10}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/" + alice).with(user("bob"))).andExpect(status().isForbidden());
    }
    @Test void csrfIsRequiredEvenForAuthenticatedWritesAndRegistration() throws Exception {
        mvc.perform(put("/api/playlists/" + playlist + "/movies/603").with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"username\":\"new\",\"password\":\"very-long-password\"}")).andExpect(status().isForbidden());
    }
    @Test void realRegistrationSessionAndLogoutWorkAndPasswordIsHashed() throws Exception {
        var response = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"username\":\"charlie\",\"password\":\"very-long-password\"}"))
                .andExpect(status().isCreated()).andReturn();
        var session = (MockHttpSession) response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/users").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("charlie"));
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username='charlie'", String.class))
                .startsWith("$2").doesNotContain("very-long-password");
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void loginRejectsWrongPasswordAndRotatesExistingSession() throws Exception {
        accounts.setLocalPassword("alice", "alice-password-long");
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"wrong-password-long\"}")).andExpect(status().isUnauthorized());
        var old = new MockHttpSession();
        var result = mvc.perform(post("/api/auth/login").session(old).with(csrf()).contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"alice-password-long\"}"))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(old.isInvalid()).isTrue();
        assertThat(result.getRequest().getSession().getId()).isNotEqualTo(old.getId());
    }
    @Test void seedProfilesCannotBeClaimedThroughRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"attacker-password\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?", String.class, alice)).isNull();
    }
    @Test void ratingUpsertChangesValueWithoutAddingVoteAndRejectsFraction() throws Exception {
        ratings.save(alice, 603, 8); ratings.save(alice, 603, 10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_rating WHERE user_id=?", Integer.class, alice)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT stars FROM user_rating WHERE user_id=?", Integer.class, alice)).isEqualTo(10);
        mvc.perform(put("/api/ratings/603").param("userId", "" + alice).with(user("alice")).with(csrf())
                .contentType("application/json").content("{\"stars\":8.5}")).andExpect(status().isBadRequest());
        ratings.remove(alice, 603);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_rating", Integer.class)).isZero();
    }
    @Test void reorderIsAtomicAndSoftDeletePreservesMemberships() {
        memberships.add(playlist, 603); memberships.add(playlist, 27205); memberships.add(playlist, 603);
        management.reorder(alice, playlist, List.of(27205L, 603L));
        assertThat(memberships.findAll(playlist)).extracting(MovieMembership::tmdbId).containsExactly(27205L, 603L);
        assertThatThrownBy(() -> management.reorder(alice, playlist, List.of(603L, 603L))).isInstanceOf(IllegalArgumentException.class);
        assertThat(memberships.findAll(playlist)).hasSize(2);
        playlists.setDeleted(playlist, alice, true);
        assertThat(playlists.findAll(alice)).isEmpty();
        assertThat(management.deleted(alice)).hasSize(1);
        playlists.setDeleted(playlist, alice, false);
        assertThat(management.export(alice, playlist).movies()).hasSize(2);
    }
    @Test void gameDoesNotLeakScoresAndRejectsReplaysAndPersistsHighscore() throws Exception {
        when(movies.details(603L)).thenReturn(movie(603, 9, 30000));
        when(movies.details(27205L)).thenReturn(movie(27205, 7, 30000));
        memberships.add(playlist, 603); memberships.add(playlist, 27205);
        var round = game.start(alice);
        String encoded = json.writeValueAsString(round);
        assertThat(encoded).doesNotContain("combinedRating", "leftScore", "rightScore", "9.0");
        var answer = game.answer(alice, round.roundId(), 603L);
        assertThat(answer.score()).isEqualTo(1);
        assertThat(answer.finished()).isTrue();
        assertThat(answer.reason()).isEqualTo("ALL_PAIRS_COMPLETED");
        assertThat(game.highscores()).contains(new GameService.Highscore("alice", 1));
        assertThatThrownBy(() -> game.answer(alice, round.roundId(), 603L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void wrongGameAnswerEndsRunAndLowVoteMoviesAreExcluded() {
        when(movies.details(603L)).thenReturn(movie(603, 9, 30000));
        when(movies.details(27205L)).thenReturn(movie(27205, 7, 30000));
        memberships.add(playlist, 603); memberships.add(playlist, 27205);
        var round = game.start(alice);
        assertThat(game.answer(alice, round.roundId(), 27205L).reason()).isEqualTo("WRONG_ANSWER");
        assertThat(game.highscores()).isEmpty();
        when(movies.details(27205L)).thenReturn(movie(27205, 7, 12));
        assertThatThrownBy(() -> game.start(alice)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test void realCsrfTokenWorksAndRefreshesAfterLogin() throws Exception {
        accounts.setLocalPassword("alice", "alice-password-long");
        var initial = mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andReturn();
        var old = (MockHttpSession) initial.getRequest().getSession(false);
        var token = json.readTree(initial.getResponse().getContentAsString());
        var login = mvc.perform(post("/api/auth/login").session(old)
                .header(token.get("csrfHeader").asText(), token.get("csrfToken").asText())
                .contentType("application/json").content("{\"username\":\"alice\",\"password\":\"alice-password-long\"}"))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var fresh = mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isOk()).andReturn();
        var next = json.readTree(fresh.getResponse().getContentAsString());
        mvc.perform(post("/api/playlists").session(session)
                .header(next.get("csrfHeader").asText(), next.get("csrfToken").asText())
                .contentType("application/json").content("{\"name\":\"Token test\",\"userId\":" + alice + "}"))
                .andExpect(status().isCreated());
    }

    @Test void apiDocumentationRequiresAuthentication() throws Exception {
        mvc.perform(get("/openapi.json")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api.html")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/openapi.json").with(user("alice"))).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").value("3.1.0"));
        mvc.perform(get("/api.html").with(user("alice"))).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
    }

    @Test void dedicatedPagesGateContentAndRedirectAuthenticatedUsers() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/auth.html"));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(forwardedUrl("/auth.html"));
        mvc.perform(get("/login").with(user("alice"))).andExpect(status().isFound()).andExpect(redirectedUrl("/"));
        mvc.perform(get("/workspace.html")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/game.html")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/game.html").with(user("alice"))).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/dashboard")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/movies/search").param("query", "Matrix")).andExpect(status().isUnauthorized());
    }

    @Test void anonymousCsrfEndpointAndHealthArePublic() throws Exception {
        mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andExpect(jsonPath("$.csrfToken").isString());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/").with(user("alice"))).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy"));
    }
}
