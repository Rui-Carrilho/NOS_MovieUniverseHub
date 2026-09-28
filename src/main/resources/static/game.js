import { api, getSession, message } from "./ui-api.js";
import { posterUrl, releaseYear } from "./format.mjs";

const $ = (selector) => document.querySelector(selector);
let activeRound = null;
let pendingRound = null;
let busy = false;

function score(value) {
  return new Intl.NumberFormat("pt-PT", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value);
}

function makeCard(movie) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "game-choice";
  const poster = document.createElement("span");
  poster.className = "game-choice-poster";
  const url = posterUrl(movie.posterUrl);
  if (url) {
    const img = document.createElement("img");
    img.src = url;
    img.alt = "";
    img.addEventListener("error", () => {
      poster.replaceChildren(document.createTextNode("Cartaz indisponível"));
    }, { once: true });
    poster.append(img);
  } else {
    poster.textContent = "Cartaz indisponível";
  }
  const name = document.createElement("strong");
  name.textContent = movie.title;
  const year = document.createElement("span");
  year.className = "game-choice-year";
  year.textContent = releaseYear(movie.releaseDate);
  button.append(poster, name, year);
  button.addEventListener("click", () => answer(movie.tmdbId));
  return button;
}

function showPair(round) {
  activeRound = round;
  pendingRound = null;
  $("#score").textContent = round.score;
  $("#round-title").textContent = "Escolhe o filme com a nota mais alta.";
  $("#game-status").textContent = "Toca num dos dois cartazes para revelar as notas.";
  $("#game-reveal").hidden = true;
  $("#next-round").hidden = true;
  $("#game-pair").replaceChildren(makeCard(round.left), makeCard(round.right));
  $("#start-game").textContent = "Recomeçar jogo ↗";
}

function showReveal(next, chosenId, previous) {
  const reveal = next.previous;
  const cards = [...$("#game-pair").children];
  const winnerId = reveal.leftScore > reveal.rightScore
    ? previous.left.tmdbId : previous.right.tmdbId;
  [reveal.leftScore, reveal.rightScore].forEach((value, index) => {
    const badge = document.createElement("span");
    badge.className = "game-choice-score";
    badge.textContent = "Nota combinada " + score(value);
    cards[index].append(badge);
    cards[index].classList.add(index === 0
      ? (winnerId === previous.left.tmdbId ? "winner" : "loser")
      : (winnerId === previous.right.tmdbId ? "winner" : "loser"));
  });
  const chosenIndex = chosenId === previous.left.tmdbId ? 0 : 1;
  cards[chosenIndex].classList.add("chosen");
  const panel = $("#game-reveal");
  panel.className = "game-reveal " + (reveal.correct ? "is-correct" : "is-wrong");
  panel.textContent = (reveal.correct
    ? "Certo! "
    : "Fim do jogo. A escolha certa era " +
      (winnerId === previous.left.tmdbId ? previous.left.title : previous.right.title) + ". ") +
    previous.left.title + " " + score(reveal.leftScore) + " · " +
    previous.right.title + " " + score(reveal.rightScore) + ".";
  panel.hidden = false;
  $("#score").textContent = next.score;
  $("#game-status").textContent = next.finished
    ? (next.reason === "ALL_PAIRS_COMPLETED"
      ? "Completaste todos os pares disponíveis. Excelente sessão!"
      : "Acertaste " + next.score + (next.score === 1 ? " par." : " pares."))
    : "Boa escolha. Continua quando quiseres.";
  if (next.finished) {
    activeRound = null;
    $("#start-game").textContent = "Jogar novamente ↗";
  } else {
    pendingRound = next;
    $("#next-round").hidden = false;
  }
  // The previous pair remains visible until the player chooses to continue.
  cards.forEach((card) => { card.disabled = true; });
}

async function loadHighscores() {
  try {
    const rows = await api("/api/game/highscores");
    const entries = rows.map((row) => {
      const item = document.createElement("li");
      const name = document.createElement("span");
      name.textContent = row.username;
      const points = document.createElement("strong");
      points.textContent = String(row.score);
      item.append(name, points);
      return item;
    });
    if (!entries.length) {
      const empty = document.createElement("li");
      empty.textContent = "Ainda não há recordes.";
      entries.push(empty);
    }
    $("#highscores").replaceChildren(...entries);
  } catch (cause) {
    const error = document.createElement("li");
    error.textContent = message(cause);
    $("#highscores").replaceChildren(error);
  }
}

async function start() {
  if (busy) return;
  busy = true;
  activeRound = null;
  pendingRound = null;
  $("#start-game").disabled = true;
  $("#next-round").hidden = true;
  $("#game-reveal").hidden = true;
  $("#game-pair").replaceChildren();
  $("#round-title").textContent = "A preparar os pares…";
  $("#game-status").textContent = "Estamos a escolher filmes das tuas playlists.";
  $("#score").textContent = "0";
  try {
    showPair(await api("/api/game/start", { method: "POST", timeoutMs: 180000 }));
  } catch (cause) {
    $("#round-title").textContent = "Ainda não foi possível começar.";
    $("#game-status").textContent = message(cause);
  } finally {
    busy = false;
    $("#start-game").disabled = false;
  }
}

async function answer(id) {
  if (busy || !activeRound) return;
  busy = true;
  const previous = activeRound;
  $("#game-pair").querySelectorAll("button").forEach((card) => { card.disabled = true; });
  $("#game-status").textContent = "A revelar as notas…";
  try {
    const next = await api("/api/game/answer", {
      method: "POST",
      body: JSON.stringify({ roundId: previous.roundId, tmdbId: id }),
    });
    showReveal(next, id, previous);
    if (next.previous.correct) void loadHighscores();
  } catch (cause) {
    $("#game-status").textContent = message(cause);
    if (cause.status === 409) {
      activeRound = null;
      $("#game-pair").replaceChildren();
      $("#start-game").textContent = "Novo jogo ↗";
    } else {
      $("#game-pair").querySelectorAll("button").forEach((card) => { card.disabled = false; });
    }
  } finally {
    busy = false;
  }
}

$("#start-game").addEventListener("click", start);
$("#next-round").addEventListener("click", () => {
  if (pendingRound && !busy) showPair(pendingRound);
});
$("#logout").addEventListener("click", async () => {
  $("#logout").disabled = true;
  try {
    await api("/api/auth/logout", { method: "POST" });
    location.replace("/login");
  } catch (cause) {
    $("#game-status").textContent = message(cause);
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
    $("#profile-name").textContent = session.user.username;
    $("#avatar").textContent = session.user.username.slice(0, 2).toUpperCase();
    $("#start-game").disabled = false;
    await loadHighscores();
  }
} catch (cause) {
  $("#game-status").textContent = message(cause);
}
