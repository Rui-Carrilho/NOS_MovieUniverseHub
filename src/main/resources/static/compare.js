import { api, message } from "./ui-api.js";
import { $, el, initShell, mapLimited, notice } from "./cinema-shell.js";

const user = await initShell();
if (user) {
  let playlists = [];
  let revision = 0;
  const format = (value) => value == null ? "Sem nota" : new Intl.NumberFormat("pt-PT", {
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }).format(value);

  function scoreCard(side, winner) {
    const card = el("article", null, "versus-card");
    if (winner) card.classList.add("is-winner");
    card.append(
      el("p", winner ? "MELHOR MÉDIA" : "PLAYLIST", "eyebrow"),
      el("h3", side.name),
      el("strong", format(side.average), "versus-score"),
      el("p", `${side.ratedMovies} de ${side.movieCount} filmes com nota`, "muted"),
    );
    if (side.unratedMovies) card.append(el("p", `${side.unratedMovies} sem votos`, "muted"));
    if (side.unavailableMovies) card.append(el("p", `${side.unavailableMovies} indisponíveis — média parcial`, "muted"));
    const link = el("a", "Ver playlist ↗", "text-action");
    link.href = `/playlists.html?id=${side.playlistId}`;
    card.append(link);
    return card;
  }

  async function movieStrip(title, ids, current) {
    const section = el("section", null, "overlap-strip");
    section.append(el("h3", `${title} (${ids.length})`));
    const list = el("div", null, "overlap-tags");
    section.append(list);
    if (!ids.length) {
      list.append(el("span", "Nenhum filme", "muted"));
      return section;
    }
    const chosen = ids.slice(0, 6);
    const names = Array(chosen.length);
    await mapLimited(chosen, async (id, index) => {
      try { names[index] = (await api(`/api/movies/${id}`)).title; }
      catch { names[index] = `Filme #${id}`; }
    });
    if (current !== revision) return section;
    names.forEach((name) => list.append(el("span", name, "overlap-tag")));
    if (ids.length > chosen.length) list.append(el("span", `+${ids.length - chosen.length} mais`, "overlap-tag more"));
    return section;
  }

  function outcome(result) {
    if (result.outcome === "WINNER") {
      const winner = result.winnerPlaylistId === result.left.playlistId ? result.left : result.right;
      return `Melhor média: ${winner.name}.`;
    }
    if (result.outcome === "TIE") return "As duas playlists estão empatadas.";
    if (result.outcome === "INCOMPLETE") return "Alguns filmes estão indisponíveis; não é possível declarar uma vencedora.";
    return "Ainda não há filmes com nota suficiente nas duas playlists.";
  }

  async function compare(left, right) {
    if (!left || !right || left === right) {
      $("#compare-status").textContent = "Escolhe duas playlists diferentes.";
      return;
    }
    const current = ++revision;
    $("#compare-button").disabled = true;
    $("#compare-result").replaceChildren();
    $("#compare-status").textContent = "A comparar os filmes e as notas…";
    try {
      const params = new URLSearchParams({ userId: String(user.id), left: String(left), right: String(right) });
      const result = await api(`/api/comparisons?${params}`, { timeoutMs: 180000 });
      if (current !== revision) return;
      const summary = el("div", null, "comparison-summary");
      summary.append(el("p", outcome(result), "comparison-outcome"));
      const cards = el("div", null, "versus-grid");
      cards.append(
        scoreCard(result.left, result.winnerPlaylistId === result.left.playlistId),
        scoreCard(result.right, result.winnerPlaylistId === result.right.playlistId),
      );
      const overlap = el("div", null, "overlap-summary");
      overlap.append(
        el("strong", `${format(result.overlapPercent)}%`, "overlap-number"),
        el("span", `${result.commonMovies.length} ${result.commonMovies.length === 1 ? "filme" : "filmes"} em comum`, "muted"),
      );
      summary.append(cards, overlap);
      $("#compare-result").append(summary);
      $("#compare-status").hidden = true;
      const strips = await Promise.all([
        movieStrip("Em comum", result.commonMovies, current),
        movieStrip("Só na primeira", result.onlyLeft, current),
        movieStrip("Só na segunda", result.onlyRight, current),
      ]);
      if (current === revision) $("#compare-result").append(...strips);
      const url = new URL(location.href);
      url.searchParams.set("left", String(left));
      url.searchParams.set("right", String(right));
      history.replaceState(null, "", url);
    } catch (cause) {
      if (current !== revision) return;
      $("#compare-status").textContent = message(cause);
      $("#compare-status").hidden = false;
    } finally { if (current === revision) $("#compare-button").disabled = playlists.length < 2; }
  }

  $("#compare-form").addEventListener("submit", (event) => {
    event.preventDefault();
    void compare(Number($("#compare-left").value), Number($("#compare-right").value));
  });
  try {
    playlists = await api(`/api/playlists?userId=${user.id}`);
    for (const playlist of playlists) {
      $("#compare-left").add(new Option(playlist.name, String(playlist.id)));
      $("#compare-right").add(new Option(playlist.name, String(playlist.id)));
    }
    if (playlists.length < 2) {
      $("#compare-status").replaceChildren(el("span", "Cria pelo menos duas playlists para as comparar. "));
      const link = el("a", "Criar playlist ↗", "text-action");
      link.href = "/playlists.html";
      $("#compare-status").append(link);
    } else {
      const params = new URLSearchParams(location.search);
      const requestedLeft = Number(params.get("left"));
      const requestedRight = Number(params.get("right"));
      const left = playlists.some((item) => item.id === requestedLeft) ? requestedLeft : playlists[0].id;
      const right = playlists.some((item) => item.id === requestedRight && item.id !== left)
        ? requestedRight : playlists.find((item) => item.id !== left).id;
      $("#compare-left").value = String(left);
      $("#compare-right").value = String(right);
      $("#compare-button").disabled = false;
      $("#compare-status").textContent = "Cada filme com nota contribui igualmente para a média da sua playlist.";
      if (params.has("left") && params.has("right")) void compare(left, right);
    }
  } catch (cause) { notice(message(cause), true); }
}
