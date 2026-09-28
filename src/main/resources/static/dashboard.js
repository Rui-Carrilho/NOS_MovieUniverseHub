import { api, getSession, message } from "./ui-api.js";
import { formatRating, releaseYear, posterUrl } from "./format.mjs";
const $ = (selector) => document.querySelector(selector);
let user;
let generation = 0;
function element(tag, value, className) {
  const node = document.createElement(tag);
  if (value != null) node.textContent = value;
  if (className) node.className = className;
  return node;
}
function notice(value) {
  $("#notice").textContent = value;
  $("#notice").hidden = !value;
}
async function pool(items, action) {
  let index = 0;
  await Promise.all(
    Array.from({ length: Math.min(3, items.length) }, async () => {
      while (index < items.length) await action(items[index++]);
    }),
  );
}
function empty(container, title, description, href, label) {
  const panel = element("div", null, "empty-panel");
  const link = element("a", label);
  link.href = href;
  panel.append(element("h3", title), element("p", description), link);
  container.replaceChildren(panel);
}
function image(url, alt) {
  const img = document.createElement("img");
  img.src = url;
  img.alt = alt;
  img.loading = "lazy";
  img.addEventListener(
    "error",
    () => img.replaceWith(element("span", "Sem cartaz", "no-poster")),
    { once: true },
  );
  return img;
}
async function loadLibrary() {
  const revision = ++generation;
  const playlists = await api("/api/playlists?userId=" + user.id);
  if (revision !== generation) return;
  $("#playlist-count").textContent = playlists.length;
  $("#playlists").replaceChildren();
  $("#collection").replaceChildren();
  if (!playlists.length) {
    empty(
      $("#playlists"),
      "Uma coleção só tua.",
      "Começa com uma lista para os filmes que queres ver ou voltar a ver.",
      "#",
      "Criar a primeira playlist ↗",
    );
    $("#playlists a").addEventListener("click", (event) => {
      event.preventDefault();
      $("#create-dialog").showModal();
    });
    empty(
      $("#collection"),
      "O próximo favorito espera por ti.",
      "Pesquisa um título e guarda-o na tua primeira playlist.",
      "/workspace.html#catalogue",
      "Descobrir filmes ↗",
    );
    $("#playlists").setAttribute("aria-busy", "false");
    $("#collection").setAttribute("aria-busy", "false");
    return;
  }
  const records = playlists.map((playlist, index) => {
    const tile = element("a", null, "playlist-tile");
    tile.href = "/workspace.html?playlist=" + playlist.id + "#library";
    const cover = element("div", null, "playlist-cover");
    cover.append(
      element("span", String(index + 1).padStart(2, "0"), "cover-empty"),
    );
    const info = element("div", null, "tile-info");
    const count = element("p", "A carregar filmes…");
    info.append(element("h3", playlist.name), count);
    tile.append(cover, info);
    $("#playlists").append(tile);
    return { playlist, cover, count, ids: [] };
  });
  await pool(records, async (record) => {
    try {
      const rows = await api(
        "/api/playlists/" + record.playlist.id + "/movies",
      );
      record.ids = rows.map((row) => row.tmdbId);
      record.count.replaceChildren(
        document.createTextNode(
          rows.length + (rows.length === 1 ? " filme" : " filmes"),
        ),
        element("span", "↗"),
      );
    } catch {
      record.count.textContent = "Não foi possível carregar os filmes";
    }
  });
  if (revision !== generation) return;
  $("#playlists").setAttribute("aria-busy", "false");
  const collection = [
    ...new Set(records.flatMap((record) => record.ids)),
  ].slice(0, 5);
  const ids = [
    ...new Set([
      ...records.flatMap((record) => record.ids.slice(0, 3)),
      ...collection,
    ]),
  ];
  const movies = new Map();
  await pool(ids, async (id) => {
    try {
      movies.set(id, await api("/api/movies/" + id));
    } catch {
      movies.set(id, null);
    }
  });
  if (revision !== generation) return;
  for (const record of records) {
    const posters = record.ids
      .slice(0, 3)
      .map((id) => movies.get(id))
      .filter((movie) => movie && posterUrl(movie.posterUrl));
    if (posters.length)
      record.cover.replaceChildren(
        ...posters.map((movie) => image(posterUrl(movie.posterUrl), "")),
      );
  }
  if (!collection.length)
    empty(
      $("#collection"),
      "Ainda há espaço para boas histórias.",
      "Adiciona filmes às tuas playlists para os encontrares aqui.",
      "/workspace.html#catalogue",
      "Descobrir filmes ↗",
    );
  for (const id of collection) {
    const movie = movies.get(id);
    const card = element("a", null, "collection-card");
    card.href = "/workspace.html?movie=" + id + "#catalogue";
    const art = element("div", null, "movie-art");
    const url = movie && posterUrl(movie.posterUrl);
    art.append(
      url
        ? image(url, "Cartaz de " + movie.title)
        : element(
            "span",
            movie ? "Cartaz indisponível" : "Filme indisponível",
            "no-poster",
          ),
    );
    if (movie && movie.tmdbVoteCount > 0)
      art.append(
        element(
          "span",
          "★ " +
            new Intl.NumberFormat("pt-PT", { maximumFractionDigits: 1 }).format(
              movie.tmdbRating,
            ),
          "movie-badge",
        ),
      );
    card.append(
      art,
      element("h3", movie?.title || "Filme #" + id),
      element(
        "p",
        movie
          ? releaseYear(movie.releaseDate) +
              " · TMDB " +
              formatRating(movie.tmdbRating, movie.tmdbVoteCount)
          : "Não foi possível obter os detalhes.",
      ),
    );
    $("#collection").append(card);
  }
  $("#collection").setAttribute("aria-busy", "false");
}
$("#new-playlist").addEventListener("click", () => {
  $("#create-error").hidden = true;
  $("#create-dialog").showModal();
});
$("#close-dialog").addEventListener("click", () => $("#create-dialog").close());
$("#create-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("[type=submit]");
  button.disabled = true;
  $("#create-error").hidden = true;
  try {
    await api("/api/playlists", {
      method: "POST",
      body: JSON.stringify({
        name: $("#playlist-name").value.trim(),
        userId: user.id,
      }),
    });
    $("#create-dialog").close();
    $("#create-form").reset();
    notice("A tua nova playlist está pronta.");
    await loadLibrary();
  } catch (cause) {
    $("#create-error").textContent = message(cause);
    $("#create-error").hidden = false;
  } finally {
    button.disabled = false;
  }
});
$("#logout").addEventListener("click", async () => {
  $("#logout").disabled = true;
  try {
    await api("/api/auth/logout", { method: "POST" });
    location.replace("/login");
  } catch (cause) {
    notice(message(cause));
    $("#logout").disabled = false;
  }
});
window.addEventListener("pageshow", (event) => {
  if (event.persisted) location.reload();
});
try {
  const session = await getSession();
  if (!session.user) location.replace("/login");
  else {
    user = session.user;
    $("#profile-name").textContent = user.username;
    $("#avatar").textContent = user.username.slice(0, 2).toUpperCase();
    $("#greeting").textContent = "Boa sessão, " + user.username + ".";
    await loadLibrary();
  }
} catch (cause) {
  notice(message(cause));
  $("#playlists").setAttribute("aria-busy", "false");
  $("#collection").setAttribute("aria-busy", "false");
  $("#playlists").replaceChildren(
    element(
      "p",
      "A biblioteca não pôde ser carregada. Atualiza a página para tentar novamente.",
      "muted",
    ),
  );
  $("#collection").replaceChildren();
}
