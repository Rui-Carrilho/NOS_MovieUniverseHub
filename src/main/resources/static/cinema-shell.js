import { api, getSession, message } from "./ui-api.js";
import { posterUrl, releaseYear } from "./format.mjs";

export const $ = (selector) => document.querySelector(selector);

export function el(tag, value, className) {
  const node = document.createElement(tag);
  if (value != null) node.textContent = value;
  if (className) node.className = className;
  return node;
}

export function notice(value, error = false) {
  const target = $("#notice");
  target.textContent = value;
  target.hidden = !value;
  target.classList.toggle("error", error);
}

export function poster(movie, className = "film-poster") {
  const box = el("span", null, className);
  const url = posterUrl(movie?.posterUrl);
  if (!url) {
    box.append(el("span", "Cartaz indisponível", "poster-fallback"));
    return box;
  }
  const image = document.createElement("img");
  image.src = url;
  image.alt = movie.title ? "Cartaz de " + movie.title : "";
  image.loading = "lazy";
  image.addEventListener("error", () => {
    box.replaceChildren(el("span", "Cartaz indisponível", "poster-fallback"));
  }, { once: true });
  box.append(image);
  return box;
}

export function movieCard(movie, open, action) {
  const card = el("article", null, "cinema-movie-card");
  const button = el("button", null, "cinema-poster-button");
  button.type = "button";
  button.setAttribute("aria-label", "Ver detalhes de " + movie.title);
  button.append(poster(movie));
  button.addEventListener("click", () => open(movie.tmdbId));
  card.append(button);
  if (action) card.append(action);
  const info = el("div", null, "cinema-movie-info");
  info.append(el("strong", movie.title), el("span", releaseYear(movie.releaseDate)));
  card.append(info);
  return card;
}

export async function mapLimited(items, worker, limit = 3) {
  let cursor = 0;
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (cursor < items.length) {
      const index = cursor++;
      await worker(items[index], index);
    }
  }));
}

export async function initShell() {
  try {
    const session = await getSession();
    if (!session.user) {
      location.replace("/login");
      return null;
    }
    const user = session.user;
    $("#profile-name").textContent = user.username;
    $("#avatar").textContent = user.username.slice(0, 2).toUpperCase();
    $("#logout").addEventListener("click", async () => {
      $("#logout").disabled = true;
      try {
        await api("/api/auth/logout", { method: "POST" });
        location.replace("/login");
      } catch (cause) {
        notice(message(cause), true);
        $("#logout").disabled = false;
      }
    });
    window.addEventListener("pageshow", (event) => {
      if (event.persisted) location.reload();
    });
    return user;
  } catch (cause) {
    notice(message(cause), true);
    return null;
  }
}
