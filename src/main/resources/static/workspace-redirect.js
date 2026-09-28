const params = new URLSearchParams(location.search);
const movie = params.get("movie");
const playlist = params.get("playlist");
let destination = "/";
if (location.hash === "#comparison") destination = "/compare.html";
else if (location.hash === "#about") destination = "/about.html";
else if (location.hash === "#library" || playlist) {
  const query = new URLSearchParams();
  if (playlist) query.set("id", playlist);
  destination = "/playlists.html" + (query.size ? `?${query}` : "");
} else if (location.hash === "#catalogue" || movie) {
  const query = new URLSearchParams();
  if (movie) query.set("movie", movie);
  destination = "/discover.html" + (query.size ? `?${query}` : "");
}
location.replace(destination);
