import { api, message } from "./ui-api.js";
import { $, el, initShell, movieCard, notice } from "./cinema-shell.js";
import { createMovieDetail } from "./cinema-movies.js";

const user = await initShell();
if (user) {
  let playlists = [];
  let selectedPlaylist = null;
  let members = new Set();
  let membershipReady = false;
  let searchRevision = 0;
  let membershipRevision = 0;
  let query = "";
  let page = 1;
  const detail = createMovieDetail({ userId: user.id, getMembership: () => ({
    playlistId: selectedPlaylist,
    contains: (id) => members.has(id),
    toggle: toggleMovie,
  }) });

  function updateStars() {
    document.querySelectorAll("[data-movie-star]").forEach((button) => {
      const id = Number(button.dataset.movieStar);
      const selected = members.has(id);
      button.textContent = selected ? "★" : "☆";
      button.setAttribute("aria-pressed", String(selected));
      button.setAttribute("aria-label", selected ? "Remover filme da playlist" : "Adicionar filme à playlist");
      button.title = selected ? "Remover da playlist" : "Adicionar à playlist";
      button.disabled = !membershipReady || !selectedPlaylist;
    });
  }

  async function toggleMovie(id) {
    if (!selectedPlaylist || !membershipReady) throw new Error("Seleciona uma playlist primeiro.");
    const playlistId = selectedPlaylist;
    const removing = members.has(id);
    await api(`/api/playlists/${playlistId}/movies/${id}`, {
      method: removing ? "DELETE" : "PUT",
    });
    if (selectedPlaylist === playlistId) {
      if (removing) members.delete(id); else members.add(id);
      updateStars();
      notice(removing ? "Filme removido da playlist." : "Filme adicionado à playlist.");
    }
  }

  function star(id) {
    const button = el("button", "☆", "cinema-star");
    button.type = "button";
    button.dataset.movieStar = String(id);
    button.addEventListener("click", async () => {
      button.disabled = true;
      try { await toggleMovie(id); }
      catch (cause) { notice(message(cause), true); }
      finally { updateStars(); }
    });
    return button;
  }

  async function selectPlaylist(id) {
    const revision = ++membershipRevision;
    selectedPlaylist = id || null;
    members = new Set();
    membershipReady = false;
    updateStars();
    if (!selectedPlaylist) return;
    try {
      const rows = await api(`/api/playlists/${selectedPlaylist}/movies`);
      if (revision !== membershipRevision) return;
      members = new Set(rows.map((row) => row.tmdbId));
      membershipReady = true;
      updateStars();
    } catch (cause) { if (revision === membershipRevision) notice(message(cause), true); }
  }

  async function search(value, nextPage = 1) {
    const current = ++searchRevision;
    query = value;
    page = nextPage;
    $("#movies").replaceChildren();
    $("#movies").setAttribute("aria-busy", "true");
    $("#pagination").hidden = true;
    $("#results-title").textContent = `Resultados para “${value}”`;
    $("#results-count").textContent = "A procurar filmes…";
    $("#search-status").hidden = true;
    try {
      const params = new URLSearchParams({ query: value, page: String(nextPage) });
      const result = await api(`/api/movies/search?${params}`);
      if (current !== searchRevision) return;
      $("#results-count").textContent = `${result.totalResults} resultados`;
      const fragment = document.createDocumentFragment();
      for (const movie of result.results) fragment.append(movieCard(movie, (id) => detail.open(id), star(movie.tmdbId)));
      $("#movies").replaceChildren(fragment);
      updateStars();
      $("#search-status").textContent = result.results.length
        ? "" : "Não encontrámos filmes. Experimenta outro título.";
      $("#search-status").hidden = !!result.results.length;
      $("#page-label").textContent = `Página ${result.page} de ${Math.max(result.totalPages, 1)}`;
      $("#previous-page").disabled = result.page <= 1;
      $("#next-page").disabled = result.page >= result.totalPages;
      $("#pagination").hidden = result.totalPages <= 1;
      const url = new URL(location.href);
      url.searchParams.set("q", value);
      url.searchParams.set("page", String(nextPage));
      history.replaceState(null, "", url);
    } catch (cause) {
      if (current !== searchRevision) return;
      $("#search-status").textContent = message(cause);
      $("#search-status").hidden = false;
    } finally { if (current === searchRevision) $("#movies").setAttribute("aria-busy", "false"); }
  }

  $("#search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const value = $("#query").value.trim();
    if (value) void search(value);
  });
  $("#previous-page").addEventListener("click", () => void search(query, page - 1));
  $("#next-page").addEventListener("click", () => void search(query, page + 1));
  $("#playlist-target").addEventListener("change", () => void selectPlaylist(Number($("#playlist-target").value)));

  try {
    playlists = await api(`/api/playlists?userId=${user.id}`);
    for (const playlist of playlists) $("#playlist-target").add(new Option(playlist.name, String(playlist.id)));
    const requested = Number(new URLSearchParams(location.search).get("playlist"));
    const chosen = playlists.some((item) => item.id === requested) ? requested : playlists[0]?.id;
    if (chosen) {
      $("#playlist-target").value = String(chosen);
      await selectPlaylist(chosen);
    } else $("#playlist-target").disabled = true;
  } catch (cause) { notice(message(cause), true); }

  const params = new URLSearchParams(location.search);
  const initial = params.get("q")?.trim();
  if (initial) {
    $("#query").value = initial;
    const requestedPage = Number(params.get("page"));
    void search(initial, Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1);
  }
  const movieId = Number(params.get("movie"));
  if (Number.isSafeInteger(movieId) && movieId > 0) void detail.open(movieId);
}
