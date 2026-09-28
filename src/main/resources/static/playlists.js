import { api, message } from "./ui-api.js";
import { $, el, initShell, mapLimited, movieCard, notice } from "./cinema-shell.js";
import { createMovieDetail } from "./cinema-movies.js";

const user = await initShell();
if (user) {
  let playlists = [];
  let selectedId = null;
  let members = [];
  let revision = 0;
  let orderIds = [];
  const movieNames = new Map();
  const detail = createMovieDetail({ userId: user.id, getMembership: () => ({
    playlistId: selectedId,
    contains: (id) => members.some((row) => row.tmdbId === id),
    toggle: toggleMovie,
  }) });

  function selected() { return playlists.find((item) => item.id === selectedId); }

  function renderPlaylists() {
    $("#playlist-count").textContent = String(playlists.length);
    const container = $("#playlist-list");
    container.replaceChildren();
    if (!playlists.length) {
      container.append(el("p", "Ainda não tens playlists. Cria a primeira para começares a tua coleção.", "cinema-empty"));
      return;
    }
    playlists.forEach((playlist, index) => {
      const button = el("button", null, "cinema-list-card");
      button.type = "button";
      button.classList.toggle("is-active", playlist.id === selectedId);
      button.append(
        el("span", String(index + 1).padStart(2, "0"), "cinema-list-number"),
        el("strong", playlist.name), el("span", "Ver coleção ↗", "cinema-list-action"),
      );
      button.addEventListener("click", () => void selectPlaylist(playlist.id));
      container.append(button);
    });
  }

  async function refresh(preferredId) {
    try {
      playlists = await api(`/api/playlists?userId=${user.id}`);
      const requested = preferredId ?? selectedId;
      selectedId = playlists.some((item) => item.id === requested) ? requested : playlists[0]?.id ?? null;
      renderPlaylists();
      $("#playlist-list").setAttribute("aria-busy", "false");
      if (selectedId) await selectPlaylist(selectedId);
      else {
        $("#selected-section").hidden = true;
        members = [];
      }
    } catch (cause) {
      $("#playlist-list").setAttribute("aria-busy", "false");
      notice(message(cause), true);
    }
  }

  function removeButton(id) {
    const button = el("button", "Remover", "cinema-remove");
    button.type = "button";
    button.setAttribute("aria-label", "Remover filme da playlist");
    button.addEventListener("click", async () => {
      button.disabled = true;
      try { await toggleMovie(id); }
      catch (cause) { notice(message(cause), true); button.disabled = false; }
    });
    return button;
  }

  async function selectPlaylist(id) {
    const current = ++revision;
    selectedId = id;
    const playlist = selected();
    if (!playlist) return;
    renderPlaylists();
    $("#selected-section").hidden = false;
    $("#selected-title").textContent = playlist.name;
    $("#selected-count").textContent = "A carregar filmes…";
    $("#add-movie-link").href = `/discover.html?playlist=${id}`;
    $("#playlist-movies").replaceChildren();
    $("#playlist-movies").setAttribute("aria-busy", "true");
    $("#movies-status").hidden = true;
    const url = new URL(location.href);
    url.searchParams.set("id", String(id));
    history.replaceState(null, "", url);
    try {
      const rows = await api(`/api/playlists/${id}/movies`);
      if (current !== revision) return;
      members = rows;
      $("#selected-count").textContent = `${rows.length} ${rows.length === 1 ? "filme" : "filmes"}`;
      if (!rows.length) {
        $("#movies-status").textContent = "Esta playlist ainda não tem filmes. Descobre um título para a começar.";
        $("#movies-status").hidden = false;
        return;
      }
      const slots = rows.map((row) => {
        const slot = el("article", `A carregar filme #${row.tmdbId}…`, "cinema-movie-card loading-card");
        $("#playlist-movies").append(slot);
        return slot;
      });
      await mapLimited(rows, async (row, index) => {
        try {
          const movie = await api(`/api/movies/${row.tmdbId}`);
          if (current !== revision) return;
          movieNames.set(row.tmdbId, movie.title);
          slots[index].replaceWith(movieCard(movie, (movieId) => detail.open(movieId), removeButton(row.tmdbId)));
        } catch (cause) {
          if (current !== revision) return;
          const unavailable = el("article", null, "cinema-movie-card unavailable-card");
          unavailable.append(el("strong", `Filme #${row.tmdbId}`), el("p", message(cause), "muted"), removeButton(row.tmdbId));
          slots[index].replaceWith(unavailable);
        }
      });
    } catch (cause) {
      if (current !== revision) return;
      $("#movies-status").textContent = message(cause);
      $("#movies-status").hidden = false;
    } finally { if (current === revision) $("#playlist-movies").setAttribute("aria-busy", "false"); }
  }

  async function toggleMovie(id) {
    if (!selectedId) throw new Error("Seleciona uma playlist.");
    const playlistId = selectedId;
    const removing = members.some((row) => row.tmdbId === id);
    await api(`/api/playlists/${playlistId}/movies/${id}`, { method: removing ? "DELETE" : "PUT" });
    if (selectedId === playlistId) {
      notice(removing ? "Filme removido da playlist." : "Filme adicionado à playlist.");
      await selectPlaylist(playlistId);
    }
  }

  $("#new-playlist").addEventListener("click", () => $("#create-dialog").showModal());
  $("#close-create").addEventListener("click", () => $("#create-dialog").close());
  $("#create-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = event.currentTarget.querySelector("[type=submit]");
    button.disabled = true;
    $("#create-error").hidden = true;
    try {
      const result = await api("/api/playlists", { method: "POST", body: JSON.stringify({
        userId: user.id, name: $("#playlist-name").value.trim(),
      }) });
      $("#create-dialog").close();
      $("#create-form").reset();
      await refresh(result.id);
      notice("Playlist criada.");
    } catch (cause) {
      $("#create-error").textContent = message(cause);
      $("#create-error").hidden = false;
    } finally { button.disabled = false; }
  });

  $("#rename-playlist").addEventListener("click", () => {
    const playlist = selected();
    if (!playlist) return;
    $("#rename-name").value = playlist.name;
    $("#rename-error").hidden = true;
    $("#rename-dialog").showModal();
  });
  $("#close-rename").addEventListener("click", () => $("#rename-dialog").close());
  $("#rename-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = selectedId;
    const button = event.currentTarget.querySelector("[type=submit]");
    button.disabled = true;
    $("#rename-error").hidden = true;
    try {
      await api(`/api/playlists/${id}`, { method: "PATCH", body: JSON.stringify({ name: $("#rename-name").value.trim() }) });
      $("#rename-dialog").close();
      await refresh(id);
      notice("Nome atualizado.");
    } catch (cause) {
      $("#rename-error").textContent = message(cause);
      $("#rename-error").hidden = false;
    } finally { button.disabled = false; }
  });

  $("#delete-playlist").addEventListener("click", async () => {
    const playlist = selected();
    if (!playlist || !confirm(`Apagar “${playlist.name}”? Podes restaurá-la depois.`)) return;
    try {
      await api(`/api/playlists/${playlist.id}?userId=${user.id}`, { method: "DELETE" });
      selectedId = null;
      await refresh();
      notice("Playlist apagada. Os filmes e notas foram preservados.");
    } catch (cause) { notice(message(cause), true); }
  });

  async function loadDeleted() {
    const target = $("#deleted-list");
    target.replaceChildren(el("p", "A carregar playlists apagadas…", "muted"));
    target.hidden = false;
    try {
      const deleted = await api("/api/playlists/deleted");
      target.replaceChildren();
      if (!deleted.length) target.append(el("p", "Não há playlists apagadas.", "muted"));
      for (const playlist of deleted) {
        const row = el("div", null, "deleted-row");
        const restore = el("button", "Restaurar", "secondary");
        restore.type = "button";
        restore.addEventListener("click", async () => {
          restore.disabled = true;
          try {
            await api(`/api/playlists/${playlist.id}/restore?userId=${user.id}`, { method: "POST" });
            await refresh(playlist.id);
            await loadDeleted();
            notice("Playlist restaurada.");
          } catch (cause) { notice(message(cause), true); restore.disabled = false; }
        });
        row.append(el("span", playlist.name), restore);
        target.append(row);
      }
    } catch (cause) { target.replaceChildren(el("p", message(cause), "muted")); }
  }
  $("#show-deleted").addEventListener("click", () => {
    if (!$("#deleted-list").hidden) $("#deleted-list").hidden = true;
    else void loadDeleted();
  });

  $("#export-playlist").addEventListener("click", async () => {
    if (!selectedId) return;
    try {
      const data = await api(`/api/playlists/${selectedId}/export`);
      const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `playlist-${selectedId}.json`;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (cause) { notice(message(cause), true); }
  });

  function renderOrder() {
    const target = $("#order-list");
    target.replaceChildren();
    orderIds.forEach((id, index) => {
      const row = el("li", null, "order-row");
      row.append(el("span", movieNames.get(id) || `Filme #${id}`));
      for (const [label, offset] of [["↑", -1], ["↓", 1]]) {
        const button = el("button", label, "secondary");
        button.type = "button";
        button.disabled = index + offset < 0 || index + offset >= orderIds.length;
        button.setAttribute("aria-label", `${offset < 0 ? "Subir" : "Descer"} ${movieNames.get(id) || `filme ${id}`}`);
        button.addEventListener("click", () => {
          [orderIds[index], orderIds[index + offset]] = [orderIds[index + offset], orderIds[index]];
          renderOrder();
        });
        row.append(button);
      }
      target.append(row);
    });
  }
  $("#reorder-playlist").addEventListener("click", () => {
    if (!selectedId) return;
    orderIds = members.map((row) => row.tmdbId);
    $("#order-error").hidden = true;
    renderOrder();
    $("#order-dialog").showModal();
  });
  $("#close-order").addEventListener("click", () => $("#order-dialog").close());
  $("#save-order").addEventListener("click", async () => {
    const button = $("#save-order");
    button.disabled = true;
    $("#order-error").hidden = true;
    try {
      await api(`/api/playlists/${selectedId}/order`, { method: "PUT", body: JSON.stringify({ movieIds: orderIds }) });
      $("#order-dialog").close();
      await selectPlaylist(selectedId);
      notice("Ordem guardada.");
    } catch (cause) {
      $("#order-error").textContent = message(cause);
      $("#order-error").hidden = false;
    } finally { button.disabled = false; }
  });

  const requested = Number(new URLSearchParams(location.search).get("id")
    || new URLSearchParams(location.search).get("playlist"));
  await refresh(Number.isSafeInteger(requested) && requested > 0 ? requested : null);
}
