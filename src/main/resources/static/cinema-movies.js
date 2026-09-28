import { api, message } from "./ui-api.js";
import { formatRating, releaseYear } from "./format.mjs";
import { $, el, notice, poster } from "./cinema-shell.js";

function score(value) {
  return value == null ? "Sem nota" : new Intl.NumberFormat("pt-PT", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function createMovieDetail({ userId, getMembership, onRatingChange }) {
  const dialog = $("#movie-dialog");
  let revision = 0;
  $("#close-details").addEventListener("click", () => dialog.close());
  dialog.addEventListener("close", () => { revision++; });

  async function renderScore(id, panel, current) {
    try {
      const result = await api(`/api/movie-scores/${id}?userId=${userId}`);
      if (revision !== current) return;
      panel.replaceChildren(
        el("h3", "Nota combinada"),
        el("strong", score(result.combinedRating), "detail-score"),
        el("p", `${result.totalVotes} votos reais · ${result.explanation}`, "muted"),
        el("p", `Perfis locais: ${score(result.localAverage)} · ${result.localVotes} votos`, "muted"),
      );
      const form = el("form", null, "detail-rating-form");
      const label = el("label", "A tua nota (1–10)");
      label.htmlFor = "detail-rating";
      const select = document.createElement("select");
      select.id = "detail-rating";
      select.required = true;
      select.add(new Option("Escolhe uma nota", ""));
      for (let stars = 1; stars <= 10; stars++) select.add(new Option(String(stars), String(stars)));
      select.value = result.userRating == null ? "" : String(result.userRating);
      const save = el("button", result.userRating == null ? "Guardar nota" : "Alterar nota", "primary");
      save.type = "submit";
      const remove = el("button", "Remover nota", "secondary");
      remove.type = "button";
      remove.disabled = result.userRating == null;
      const feedback = el("p", "", "form-error");
      feedback.hidden = true;
      feedback.setAttribute("role", "alert");
      form.append(label, select, save, remove, feedback);
      panel.append(form);
      let writing = false;
      async function changeRating(method) {
        if (writing) return;
        writing = true;
        save.disabled = true;
        remove.disabled = true;
        try {
          await api(`/api/ratings/${id}?userId=${userId}`, {
            method,
            ...(method === "PUT" ? { body: JSON.stringify({ stars: Number(select.value) }) } : {}),
          });
          if (revision !== current) return;
          notice(method === "PUT" ? "Nota guardada." : "Nota removida.");
          onRatingChange?.();
          await renderScore(id, panel, current);
        } catch (cause) {
          feedback.textContent = message(cause);
          feedback.hidden = false;
        } finally {
          writing = false;
          save.disabled = false;
          remove.disabled = result.userRating == null;
        }
      }
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (select.value) void changeRating("PUT");
      });
      remove.addEventListener("click", () => void changeRating("DELETE"));
    } catch (cause) {
      if (revision !== current) return;
      panel.replaceChildren(el("p", message(cause), "muted"));
      const retry = el("button", "Tentar novamente", "secondary");
      retry.type = "button";
      retry.addEventListener("click", () => void renderScore(id, panel, current));
      panel.append(retry);
    }
  }

  async function open(id) {
    const current = ++revision;
    const body = $("#detail-body");
    body.replaceChildren(el("p", "A carregar detalhes…", "muted"));
    if (!dialog.open) dialog.showModal();
    try {
      const movie = await api(`/api/movies/${id}`);
      if (revision !== current) return;
      const copy = el("div", null, "detail-copy");
      const title = el("h2", movie.title);
      title.id = "detail-title";
      copy.append(
        el("p", releaseYear(movie.releaseDate), "eyebrow"), title,
        el("p", movie.genres?.length ? movie.genres.join(" · ") : "Géneros indisponíveis", "muted"),
        el("p", movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : "Duração indisponível", "muted"),
        el("p", "TMDB: " + formatRating(movie.tmdbRating, movie.tmdbVoteCount), "detail-tmdb"),
        el("p", movie.overview?.trim() || "Sinopse indisponível.", "detail-overview"),
      );
      const membership = getMembership?.();
      if (membership?.playlistId) {
        const toggle = el("button", "", "secondary detail-membership");
        toggle.type = "button";
        const update = () => {
          toggle.textContent = membership.contains(id)
            ? "★ Remover da playlist" : "☆ Adicionar à playlist";
        };
        update();
        toggle.addEventListener("click", async () => {
          toggle.disabled = true;
          try { await membership.toggle(id); update(); }
          catch (cause) { notice(message(cause), true); }
          finally { toggle.disabled = false; }
        });
        copy.append(toggle);
      }
      const rating = el("section", "A carregar notas…", "detail-rating");
      copy.append(rating);
      body.replaceChildren(poster(movie, "detail-poster"), copy);
      await renderScore(id, rating, current);
    } catch (cause) {
      if (revision !== current) return;
      const heading = el("h2", "Detalhes indisponíveis");
      heading.id = "detail-title";
      body.replaceChildren(heading, el("p", message(cause)));
    }
  }
  return { open };
}
