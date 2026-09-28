import {formatRating, releaseYear, posterUrl} from './format.mjs';

const $ = selector => document.querySelector(selector);
let csrf = null;
let gameRevision = 0;
let orderIds = [];
let orderLabels = new Map();
const state = {
  users: [], playlists: [], userId: null, playlistId: null, members: new Set(),
  context: 0, view: 0, detail: 0, userLoad: 0, membershipReady: false,
  mode: 'search', query: '', page: 1, pages: 0, pending: new Set(),
  searchAbort: null, detailAbort: null, comparison: 0
};

function text(tag, value, className) {
  const element = document.createElement(tag);
  element.textContent = value;
  if (className) element.className = className;
  return element;
}
function notify(message, error = false) {
  $('#notice').textContent = message;
  $('#notice').hidden = !message;
  $('#notice').classList.toggle('error', error);
}
function catalogueMessage(message) {
  $('#catalogue-status').replaceChildren(text('p', message));
  $('#catalogue-status').hidden = !message;
}

async function api(path, options = {}) {
  const {timeoutMs = 20000, ...requestOptions} = options;
  const mutating = !['GET', 'HEAD'].includes((options.method || 'GET').toUpperCase());
  if (mutating && !csrf) {
    const response = await fetch('/api/auth/session', {signal: AbortSignal.timeout(20000)});
    if (!response.ok) throw new Error('Não foi possível verificar a sessão.');
    csrf = await response.json();
  }
  const response = await fetch(path, {
    ...requestOptions,
    signal: options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(timeoutMs)]) : AbortSignal.timeout(timeoutMs),
    headers: {'Accept': 'application/json', ...(options.body ? {'Content-Type': 'application/json'} : {}),
      ...(mutating ? {[csrf.csrfHeader]: csrf.csrfToken} : {})}
  });
  if (!response.ok) {
    let body;
    try { body = await response.json(); } catch { /* Safe generic fallback. */ }
    if (response.status === 401 || response.status === 403) csrf = null;
    if (response.status === 401) location.replace('/login');
    throw new Error(body?.message || `Não foi possível concluir o pedido (${response.status}).`);
  }
  const body = await response.text();
  return body ? JSON.parse(body) : null;
}
function errorMessage(error) {
  return error.name === 'TimeoutError' ? 'O pedido demorou demasiado. Tenta novamente.'
    : error instanceof TypeError ? 'Não foi possível contactar a aplicação. Confirma que está a funcionar.' : error.message;
}

async function loadUsers() {
  try {
    csrf = await api('/api/auth/session');
    const user = csrf.user;
    if (!user) { location.replace("/login"); return; }
    state.users = user ? [user] : [];
    $('#user-select').replaceChildren(new Option(user ? user.username : 'Inicia sessão', user ? user.id : ''));
    $('#user-select').disabled = true;
    $('#login-form').hidden = !!user;
    $('#user-form').hidden = !!user;
    $('#logout').hidden = !user;
    $('#auth-info').textContent = user ? `Sessão de ${user.username}` : 'Entra ou regista uma nova conta abaixo.';
    $('#show-deleted').disabled = !user;
    $('#start-game').disabled = !user;
    ++gameRevision;
    $('#game-pair').replaceChildren();
    $('#game-status').textContent = user ? 'Pronto para jogar.' : 'Inicia sessão para jogar.';
    $('#deleted-playlists').replaceChildren();
    $('#order-editor').hidden = true;
    await selectUser(user?.id ?? null);
    const destination = new URLSearchParams(location.search);
    const playlistId = Number(destination.get('playlist'));
    const movieId = Number(destination.get('movie'));
    if (Number.isSafeInteger(playlistId) && state.playlists.some(p => p.id === playlistId)) await selectPlaylist(playlistId, true);
    if (Number.isSafeInteger(movieId) && movieId > 0) await showDetails(movieId);
    $('#highscores').replaceChildren();
  } catch (error) { notify(errorMessage(error), true); }
}
async function selectUser(userId) {
  state.userId = userId;
  if ($('#movie-dialog').open) $('#movie-dialog').close();
  resetComparison();
  const revision = ++state.context;
  state.playlistId = null;
  updateManagement();
  state.members.clear();
  state.membershipReady = false;
  state.playlists = [];
  $('#playlist-select').replaceChildren(new Option('Seleciona uma playlist', ''));
  $('#playlist-select').disabled = true;
  $('#open-playlist').disabled = true;
  $('#playlist-form button').disabled = !userId;
  $('#playlist-cards').setAttribute('aria-busy', 'true');
  updateStars();
  if (state.mode === 'playlist') clearView('Seleciona uma playlist para ver os filmes.');
  if (!userId) {
    $('#playlist-cards').replaceChildren(text('p', 'Cria um perfil para começar a tua coleção.', 'muted'));
    $('#playlist-cards').setAttribute('aria-busy', 'false');
    return;
  }
  $('#playlist-cards').replaceChildren(text('p', 'A carregar playlists…', 'muted'));
  try {
    const playlists = await api(`/api/playlists?userId=${userId}`);
    if (revision !== state.context) return;
    state.playlists = playlists;
    fillComparisonOptions();
    $('#playlist-cards').replaceChildren();
    playlists.forEach((playlist, index) => {
      $('#playlist-select').add(new Option(playlist.name, playlist.id));
      const button = text('button', '', 'playlist-card');
      button.type = 'button';
      button.append(text('span', String(index + 1).padStart(2, '0'), 'list-number'), text('strong', playlist.name), text('span', 'Ver coleção ↗', 'muted'));
      button.addEventListener('click', () => selectPlaylist(playlist.id, true));
      $('#playlist-cards').append(button);
    });
    if (!playlists.length) $('#playlist-cards').append(text('p', 'Ainda não tens playlists. Cria a primeira abaixo.', 'muted'));
    $('#playlist-select').disabled = !playlists.length;
    if (playlists.length) await selectPlaylist(playlists[0].id);
  } catch (error) {
    if (revision === state.context) {
      $('#playlist-cards').replaceChildren(text('p', 'Não foi possível carregar as playlists.', 'muted'));
      notify(errorMessage(error), true);
    }
  } finally { $('#playlist-cards').setAttribute('aria-busy', 'false'); }
}

async function selectPlaylist(playlistId, open = false) {
  const revision = ++state.context;
  state.playlistId = playlistId;
  updateManagement();
  state.members.clear();
  state.membershipReady = false;
  $('#playlist-select').value = playlistId ? String(playlistId) : '';
  $('#open-playlist').disabled = true;
  updateStars();
  if (state.mode === 'playlist') clearView('A carregar a coleção…');
  if (!playlistId) return;
  try {
    const memberships = await api(`/api/playlists/${playlistId}/movies`);
    if (revision !== state.context) return;
    state.members = new Set(memberships.map(movie => movie.tmdbId));
    state.membershipReady = true;
    $('#open-playlist').disabled = false;
    updateStars();
    if (open || state.mode === 'playlist') showPlaylist(memberships);
  } catch (error) { if (revision === state.context) notify(errorMessage(error), true); }
}

function updateStars() {
  const playlist = state.playlists.find(item => item.id === state.playlistId);
  document.querySelectorAll('[data-star]').forEach(button => {
    const id = Number(button.dataset.star);
    const belongs = state.members.has(id);
    button.textContent = belongs ? '★' : '☆';
    button.setAttribute('aria-pressed', String(belongs));
    const label = playlist ? `${belongs ? 'Remover de' : 'Adicionar a'} ${playlist.name}` : 'Seleciona uma playlist';
    button.setAttribute('aria-label', label);
    button.title = label;
    button.disabled = !state.membershipReady || !playlist || state.pending.has(`${playlist.id}:${id}`);
  });
}

function star(id) {
  const button = text('button', '☆', 'star');
  button.type = 'button';
  button.dataset.star = id;
  button.addEventListener('click', () => toggleMembership(id));
  return button;
}

async function toggleMembership(id) {
  const playlistId = state.playlistId;
  const context = state.context;
  const key = `${playlistId}:${id}`;
  if (!playlistId || !state.membershipReady || state.pending.has(key)) return;
  const removing = state.members.has(id);
  state.pending.add(key);
  updateStars();
  try {
    await api(`/api/playlists/${playlistId}/movies/${id}`, {method: removing ? 'DELETE' : 'PUT'});
    if (context !== state.context) return;
    if (removing) state.members.delete(id); else state.members.add(id);
    ++state.comparison;
    $('#comparison-result').replaceChildren();
    $('#comparison-status').textContent = 'A playlist mudou. Compara novamente para atualizar o resultado.';
    notify(removing ? 'Filme removido da playlist.' : 'Filme adicionado à playlist.');
    if (removing && state.mode === 'playlist') {
      document.querySelectorAll(`#movies [data-movie="${id}"]`).forEach(card => card.remove());
      $('#result-count').textContent = `${state.members.size} filmes`;
      if (!state.members.size) catalogueMessage('Esta playlist ainda não tem filmes. Pesquisa um título para começar.');
    }
  } catch (error) { if (context === state.context) notify(errorMessage(error), true); }
  finally { state.pending.delete(key); updateStars(); }
}

function poster(movie) {
  const holder = text('div', '', 'poster');
  const placeholder = text('span', 'Sem imagem', 'poster-placeholder');
  const url = posterUrl(movie.posterUrl);
  if (url) {
    const img = document.createElement('img');
    img.src = url; img.alt = `Cartaz de ${movie.title}`; img.loading = 'lazy';
    img.addEventListener('error', () => holder.replaceChildren(placeholder), {once: true});
    holder.append(img);
  } else holder.append(placeholder);
  return holder;
}

function movieCard(movie) {
  const card = text('article', '', 'movie-card');
  card.dataset.movie = movie.tmdbId;
  const open = text('button', '', 'poster-button');
  open.type = 'button'; open.setAttribute('aria-label', `Ver detalhes de ${movie.title}`);
  open.append(poster(movie)); open.addEventListener('click', () => showDetails(movie.tmdbId));
  const content = text('div', '', 'movie-content');
  content.append(text('p', releaseYear(movie.releaseDate), 'year'), text('h4', movie.title),
    text('p', formatRating(movie.tmdbRating, movie.tmdbVoteCount), 'rating'));
  card.append(open, star(movie.tmdbId), content);
  return card;
}

function clearView(message) {
  ++state.view;
  state.searchAbort?.abort();
  $('#movies').replaceChildren();
  $('#movies').setAttribute('aria-busy', 'false');
  $('#pagination').hidden = true;
  $('#result-count').textContent = '';
  catalogueMessage(message);
}

async function search(query, page = 1) {
  clearView('A procurar filmes…');
  state.mode = 'search'; state.query = query; state.page = page;
  const revision = state.view;
  state.searchAbort = new AbortController();
  $('#results-title').textContent = `Resultados para “${query}”`;
  $('#movies').setAttribute('aria-busy', 'true');
  try {
    const result = await api(`/api/movies/search?${new URLSearchParams({query, page: String(page)})}`, {signal: state.searchAbort.signal});
    if (revision !== state.view) return;
    state.pages = result.totalPages;
    result.results.forEach(movie => $('#movies').append(movieCard(movie)));
    $('#result-count').textContent = `${result.totalResults} resultados`;
    catalogueMessage(result.results.length ? '' : 'Não encontrámos filmes. Experimenta outro título.');
    $('#page-label').textContent = `Página ${result.page} de ${Math.max(result.totalPages, 1)}`;
    $('#previous-page').disabled = result.page <= 1;
    $('#next-page').disabled = result.page >= result.totalPages;
    $('#pagination').hidden = result.totalPages <= 1;
    updateStars();
  } catch (error) {
    if (revision === state.view && error.name !== 'AbortError') catalogueMessage(errorMessage(error));
  } finally { if (revision === state.view) $('#movies').setAttribute('aria-busy', 'false'); }
}

async function showPlaylist(memberships) {
  clearView('A carregar os filmes da coleção…');
  state.mode = 'playlist';
  const revision = state.view;
  const playlist = state.playlists.find(item => item.id === state.playlistId);
  $('#results-title').textContent = playlist?.name ?? 'A tua playlist';
  $('#result-count').textContent = `${memberships.length} filmes`;
  $('#catalogue').scrollIntoView({behavior: 'smooth', block: 'start'});
  if (!memberships.length) { catalogueMessage('Esta playlist ainda não tem filmes. Pesquisa um título para começar.'); return; }
  catalogueMessage('');
  const slots = memberships.map(member => {
    const slot = text('article', `A carregar filme #${member.tmdbId}…`, 'movie-card unavailable');
    slot.dataset.movie = member.tmdbId;
    $('#movies').append(slot); return slot;
  });
  // Three workers preserve stored order and bound calls for a large imported playlist.
  let cursor = 0;
  async function worker() {
    while (cursor < memberships.length && revision === state.view) {
      const index = cursor++;
      const id = memberships[index].tmdbId;
      try {
        const movie = await api(`/api/movies/${id}`);
        if (revision !== state.view) return;
        slots[index].replaceWith(movieCard(movie));
      } catch (error) {
        if (revision !== state.view) return;
        slots[index].replaceChildren(text('h4', `Filme #${id}`), text('p', errorMessage(error)), star(id));
      }
      updateStars();
    }
  }
  $('#movies').setAttribute('aria-busy', 'true');
  await Promise.all(Array.from({length: Math.min(3, memberships.length)}, worker));
  if (revision === state.view) $('#movies').setAttribute('aria-busy', 'false');
}

async function showDetails(id) {
  const revision = ++state.detail;
  state.detailAbort?.abort(); state.detailAbort = new AbortController();
  const body = $('#detail-body');
  body.replaceChildren(text('h2', 'Detalhes do filme'), text('p', 'A carregar…'));
  body.querySelector('h2').id = 'detail-title';
  if (!$('#movie-dialog').open) $('#movie-dialog').showModal();
  try {
    const movie = await api(`/api/movies/${id}`, {signal: state.detailAbort.signal});
    if (revision !== state.detail) return;
    const heading = text('h2', movie.title); heading.id = 'detail-title';
    const info = text('div', '', 'detail-info');
    info.append(text('p', releaseYear(movie.releaseDate), 'eyebrow'), heading,
      text('p', movie.genres.length ? movie.genres.join(' · ') : 'Géneros indisponíveis', 'muted'),
      text('p', movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : 'Duração indisponível', 'muted'),
      text('p', `TMDB: ${formatRating(movie.tmdbRating, movie.tmdbVoteCount)}`, 'rating'),
      text('p', movie.overview?.trim() || 'Sinopse indisponível.', 'synopsis'), star(movie.tmdbId));
    const ratingPanel = text('section', 'A carregar notas…', 'rating-panel');
    info.append(ratingPanel);
    body.replaceChildren(poster(movie), info);
    updateStars();
    await loadScore(id, ratingPanel, revision);
  } catch (error) {
    if (revision === state.detail && error.name !== 'AbortError') body.replaceChildren(text('h2', 'Detalhes indisponíveis'), text('p', errorMessage(error)));
    const title = body.querySelector('h2'); if (title) title.id = 'detail-title';
  }
}

$('#search-form').addEventListener('submit', event => {
  event.preventDefault();
  const query = $('#query').value.trim();
  if (query) search(query);
});
$('#previous-page').addEventListener('click', () => search(state.query, state.page - 1));
$('#next-page').addEventListener('click', () => search(state.query, state.page + 1));
$('#user-select').addEventListener('change', () => selectUser(Number($('#user-select').value) || null));
$('#playlist-select').addEventListener('change', () => selectPlaylist(Number($('#playlist-select').value) || null));
$('#open-playlist').addEventListener('click', () => selectPlaylist(state.playlistId, true));
$('#refresh-library').addEventListener('click', () => loadUsers());
$('#close-details').addEventListener('click', () => $('#movie-dialog').close());
$('#movie-dialog').addEventListener('close', () => { ++state.detail; state.detailAbort?.abort(); });

$('#user-form').addEventListener('submit', async event => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button'); button.disabled = true;
  try {
    await api('/api/auth/register', {method: 'POST', body: JSON.stringify({username: $('#username').value.trim(), password: $('#register-password').value})});
    $('#username').value = ''; $('#register-password').value = ''; csrf = null;
    notify('Conta criada.'); await loadUsers();
  } catch (error) { notify(errorMessage(error), true); }
  finally { button.disabled = false; }
});
$('#playlist-form').addEventListener('submit', async event => {
  event.preventDefault();
  const userId = state.userId;
  if (!userId) return;
  const button = event.currentTarget.querySelector('button'); button.disabled = true;
  try {
    const playlist = await api('/api/playlists', {method: 'POST', body: JSON.stringify({name: $('#playlist-name').value.trim(), userId})});
    if (userId !== state.userId) return;
    $('#playlist-name').value = ''; notify('Playlist criada.');
    await selectUser(userId);
    if (userId === state.userId) await selectPlaylist(playlist.id, true);
  } catch (error) { notify(errorMessage(error), true); }
  finally { button.disabled = !state.userId; }
});
loadUsers();


function formatScore(value) {
  return value == null ? 'Sem nota' : new Intl.NumberFormat('pt-PT', {
    minimumFractionDigits: 2, maximumFractionDigits: 2
  }).format(value);
}

async function loadScore(id, panel, detailRevision) {
  const userId = state.userId;
  try {
    const suffix = userId ? `?userId=${userId}` : '';
    const score = await api(`/api/movie-scores/${id}${suffix}`);
    if (detailRevision !== state.detail || userId !== state.userId) return;
    panel.replaceChildren();
    panel.append(text('h3', 'Nota combinada'),
      text('p', `${formatScore(score.combinedRating)} · ${score.totalVotes} votos reais`, 'combined-score'),
      text('p', score.explanation, 'muted'),
      text('p', `Perfis locais: ${formatScore(score.localAverage)} · ${score.localVotes} votos`, 'muted'));
    if (!userId) {
      panel.append(text('p', 'Seleciona um perfil local para dar uma nota.'));
      return;
    }
    const form = document.createElement('form');
    form.className = 'rating-form';
    const label = text('label', 'A tua nota');
    label.htmlFor = 'own-rating';
    const select = document.createElement('select');
    select.id = 'own-rating'; select.required = true;
    select.add(new Option('Escolhe de 1 a 10', ''));
    for (let stars = 1; stars <= 10; stars++) select.add(new Option(String(stars), String(stars)));
    select.value = score.userRating == null ? '' : String(score.userRating);
    const save = text('button', score.userRating == null ? 'Guardar nota' : 'Alterar nota');
    save.type = 'submit';
    const remove = text('button', 'Remover nota', 'quiet');
    remove.type = 'button'; remove.disabled = score.userRating == null;
    const feedback = text('p', '', 'muted'); feedback.setAttribute('role', 'status');
    form.append(label, select, save, remove);
    panel.append(form, feedback);
    let writing = false;
    async function changeRating(method) {
      if (writing) return;
      writing = true; save.disabled = true; remove.disabled = true; select.disabled = true;
      try {
        const body = method === 'PUT' ? JSON.stringify({stars: Number(select.value)}) : undefined;
        await api(`/api/ratings/${id}?userId=${userId}`, {method, body});
        if (detailRevision !== state.detail || userId !== state.userId) return;
        notify(method === 'PUT' ? 'Nota guardada.' : 'Nota removida.');
        ++state.comparison;
        $('#comparison-result').replaceChildren();
        $('#comparison-status').textContent = 'As notas mudaram. Compara novamente para atualizar o resultado.';
        await loadScore(id, panel, detailRevision);
      } catch (error) {
        if (detailRevision === state.detail && userId === state.userId) feedback.textContent = errorMessage(error);
      } finally {
        writing = false; save.disabled = false; remove.disabled = score.userRating == null; select.disabled = false;
      }
    }
    form.addEventListener('submit', event => { event.preventDefault(); if (select.value) changeRating('PUT'); });
    remove.addEventListener('click', () => changeRating('DELETE'));
  } catch (error) {
    if (detailRevision === state.detail && userId === state.userId) {
      const retry = text('button', 'Tentar carregar notas novamente'); retry.type = 'button';
      retry.addEventListener('click', () => loadScore(id, panel, detailRevision));
      panel.replaceChildren(text('p', errorMessage(error)), retry);
    }
  }
}

function resetComparison() {
  ++state.comparison;
  $('#compare-left').replaceChildren(new Option('Seleciona uma playlist', ''));
  $('#compare-right').replaceChildren(new Option('Seleciona uma playlist', ''));
  $('#comparison-form button').disabled = true;
  $('#comparison-result').replaceChildren();
  $('#comparison-status').textContent = 'Cria pelo menos duas playlists no mesmo perfil.';
}

function fillComparisonOptions() {
  state.playlists.forEach(playlist => {
    $('#compare-left').add(new Option(playlist.name, playlist.id));
    $('#compare-right').add(new Option(playlist.name, playlist.id));
  });
  const enough = state.playlists.length >= 2;
  $('#comparison-form button').disabled = !enough;
  if (enough) {
    $('#compare-left').value = String(state.playlists[0].id);
    $('#compare-right').value = String(state.playlists[1].id);
    $('#comparison-status').textContent = 'Cada filme com nota contribui igualmente para a média da sua playlist.';
  }
}

function comparisonCard(side) {
  const card = text('article', '', 'comparison-card');
  card.append(text('h3', side.name), text('p', formatScore(side.average), 'combined-score'),
    text('p', `${side.ratedMovies} de ${side.movieCount} filmes com nota`, 'muted'));
  if (side.unratedMovies) card.append(text('p', `${side.unratedMovies} sem votos`, 'muted'));
  if (side.unavailableMovies) card.append(text('p', `${side.unavailableMovies} indisponíveis — média parcial`, 'muted'));
  return card;
}

$('#comparison-form').addEventListener('submit', async event => {
  event.preventDefault();
  const left = Number($('#compare-left').value), right = Number($('#compare-right').value);
  if (!left || !right || left === right) {
    $('#comparison-status').textContent = 'Escolhe duas playlists diferentes.';
    return;
  }
  const revision = ++state.comparison;
  const userId = state.userId;
  const button = $('#comparison-form button');
  button.disabled = true;
  $('#comparison-result').replaceChildren();
  $('#comparison-status').textContent = 'A comparar as notas dos filmes…';
  try {
    const query = new URLSearchParams({userId, left, right});
    const result = await api(`/api/comparisons?${query}`, {timeoutMs: 180000});
    if (revision !== state.comparison || userId !== state.userId) return;
    const cards = text('div', '', 'comparison-grid');
    cards.append(comparisonCard(result.left), comparisonCard(result.right));
    let message;
    if (result.outcome === 'WINNER') {
      const winner = result.winnerPlaylistId === result.left.playlistId ? result.left : result.right;
      message = `Melhor média: ${winner.name}.`;
    } else if (result.outcome === 'TIE') message = 'As playlists estão empatadas.';
    else if (result.outcome === 'INCOMPLETE') message = 'Dados incompletos: não é possível determinar uma vencedora. Tenta novamente.';
    else message = 'Não há filmes com nota suficiente nas duas playlists para comparar.';
    $('#comparison-status').textContent = message;
    const overlap = text('p', `${result.commonMovies.length} filmes em comum · ${formatScore(result.overlapPercent)}% de sobreposição.`, 'muted');
    const unique = text('p', `${result.onlyLeft.length} exclusivos da primeira · ${result.onlyRight.length} exclusivos da segunda.`, 'muted');
    $('#comparison-result').append(cards, overlap, unique);
  } catch (error) {
    if (revision === state.comparison && userId === state.userId) $('#comparison-status').textContent = errorMessage(error);
  } finally { if (revision === state.comparison && userId === state.userId) button.disabled = state.playlists.length < 2; }
});

function updateManagement() {
  ['rename-playlist', 'delete-playlist', 'order-playlist', 'export-playlist'].forEach(id => {
    $('#' + id).disabled = !state.playlistId;
  });
  $('#order-editor').hidden = true;
}
$('#login-form').addEventListener('submit', async event => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button'); button.disabled = true;
  try {
    await api('/api/auth/login', {method: 'POST', body: JSON.stringify({
      username: $('#login-name').value.trim(), password: $('#login-password').value
    })});
    $('#login-password').value = ''; csrf = null; notify('Sessão iniciada.'); await loadUsers();
  } catch (error) { notify(errorMessage(error), true); }
  finally { button.disabled = false; }
});
$('#logout').addEventListener('click', async () => {
  try { await api('/api/auth/logout', {method: 'POST'}); csrf = null; await loadUsers(); notify('Sessão terminada.'); }
  catch (error) { notify(errorMessage(error), true); }
});
$('#rename-playlist').addEventListener('click', async () => {
  const playlist = state.playlists.find(p => p.id === state.playlistId);
  if (!playlist) return;
  const name = window.prompt('Novo nome da playlist', playlist.name);
  if (!name?.trim()) return;
  try {
    await api(`/api/playlists/${playlist.id}`, {method: 'PATCH', body: JSON.stringify({name})});
    await selectUser(state.userId); await selectPlaylist(playlist.id); notify('Nome atualizado.');
  } catch (error) { notify(errorMessage(error), true); }
});
$('#delete-playlist').addEventListener('click', async () => {
  const playlist = state.playlists.find(p => p.id === state.playlistId);
  if (!playlist || !window.confirm(`Apagar “${playlist.name}”? Podes restaurá-la depois.`)) return;
  try {
    await api(`/api/playlists/${playlist.id}?userId=${state.userId}`, {method: 'DELETE'});
    await selectUser(state.userId); notify('Playlist apagada. Os filmes e notas foram preservados.');
  } catch (error) { notify(errorMessage(error), true); }
});
$('#show-deleted').addEventListener('click', loadDeleted);
async function loadDeleted() {
  try {
    const deleted = await api('/api/playlists/deleted');
    const container = $('#deleted-playlists'); container.replaceChildren();
    if (!deleted.length) container.append(text('p', 'Não há playlists apagadas.'));
    deleted.forEach(playlist => {
      const line = text('p', playlist.name + ' ');
      const button = text('button', 'Restaurar'); button.type = 'button';
      button.addEventListener('click', async () => {
        button.disabled = true;
        try {
          await api(`/api/playlists/${playlist.id}/restore?userId=${state.userId}`, {method: 'POST'});
          await selectUser(state.userId); await loadDeleted();
        } catch (error) { button.disabled = false; notify(errorMessage(error), true); }
      });
      line.append(button); container.append(line);
    });
  } catch (error) { notify(errorMessage(error), true); }
}
$('#export-playlist').addEventListener('click', async () => {
  try {
    const data = await api(`/api/playlists/${state.playlistId}/export`);
    const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'}));
    const link = document.createElement('a'); link.href = url; link.download = `playlist-${data.playlist.id}.json`;
    link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) { notify(errorMessage(error), true); }
});
let orderPlaylistId = null;
$('#order-playlist').addEventListener('click', async () => {
  const id = state.playlistId;
  try {
    const memberships = await api(`/api/playlists/${id}/movies`);
    if (id !== state.playlistId) return;
    orderPlaylistId = id; orderIds = memberships.map(m => m.tmdbId); orderLabels = new Map();
    const pending = [...orderIds];
    await Promise.all(Array.from({length: Math.min(3, pending.length)}, async () => {
      while (pending.length) {
        const movieId = pending.shift();
        try {
          const movie = await api(`/api/movies/${movieId}`);
          orderLabels.set(movieId, `${movie.title} (${releaseYear(movie.releaseDate)})`);
        } catch { orderLabels.set(movieId, `Filme indisponível (#${movieId})`); }
      }
    }));
    if (id !== state.playlistId) return;
    renderOrder(); $('#order-editor').hidden = false;
  } catch (error) { notify(errorMessage(error), true); }
});
function renderOrder() {
  $('#order-list').replaceChildren();
  orderIds.forEach((id, index) => {
    const line = text('li', (orderLabels.get(id) || `Filme #${id}`) + ' ');
    for (const [label, step] of [['↑', -1], ['↓', 1]]) {
      const button = text('button', label); button.type = 'button';
      button.setAttribute('aria-label', `${step < 0 ? 'Subir' : 'Descer'} filme ${id}`);
      button.disabled = index + step < 0 || index + step >= orderIds.length;
      button.addEventListener('click', () => {
        [orderIds[index], orderIds[index + step]] = [orderIds[index + step], orderIds[index]];
        renderOrder();
      });
      line.append(button);
    }
    $('#order-list').append(line);
  });
}
$('#save-order').addEventListener('click', async () => {
  const id = orderPlaylistId;
  if (!id || id !== state.playlistId) return;
  const button = $('#save-order'); button.disabled = true;
  try {
    await api(`/api/playlists/${id}/order`, {method: 'PUT', body: JSON.stringify({movieIds: orderIds})});
    $('#order-editor').hidden = true; await selectPlaylist(id, true); notify('Ordem guardada.');
  } catch (error) { notify(errorMessage(error), true); }
  finally { button.disabled = false; }
});
$('#cancel-order').addEventListener('click', () => { $('#order-editor').hidden = true; });
async function loadHighscores() {
  try {
    const rows = await api('/api/game/highscores');
    $('#highscores').replaceChildren(...rows.map(row => text('li', `${row.username}: ${row.score}`)));
    if (!rows.length) $('#highscores').append(text('li', 'Ainda não há recordes.'));
  } catch (error) { $('#highscores').replaceChildren(text('li', errorMessage(error))); }
}
$('#start-game').addEventListener('click', async () => {
  const revision = ++gameRevision;
  $('#start-game').disabled = true; $('#game-pair').replaceChildren();
  $('#game-status').textContent = 'A preparar os pares…';
  try {
    const result = await api('/api/game/start', {method: 'POST', timeoutMs: 180000});
    if (revision === gameRevision) renderGame(result, revision);
  } catch (error) { if (revision === gameRevision) $('#game-status').textContent = errorMessage(error); }
  finally { if (revision === gameRevision) $('#start-game').disabled = !state.userId; }
});
function renderGame(result, revision) {
  const previous = result.previous ? `Par anterior: ${formatScore(result.previous.leftScore)} / ${formatScore(result.previous.rightScore)}. ` : '';
  $('#game-status').textContent = previous + `Acertos: ${result.score}. ` +
    (result.finished ? (result.reason === 'ALL_PAIRS_COMPLETED' ? 'Completaste todos os pares!' : 'Fim do jogo.') : 'Escolhe o filme com a nota mais alta.');
  $('#game-pair').replaceChildren();
  if (result.finished) { loadHighscores(); return; }
  for (const movie of [result.left, result.right]) {
    const button = text('button', '', 'game-card'); button.type = 'button';
    button.append(poster(movie), text('strong', movie.title), text('span', releaseYear(movie.releaseDate)));
    button.addEventListener('click', async () => {
      $('#game-pair').querySelectorAll('button').forEach(b => b.disabled = true);
      try {
        const next = await api('/api/game/answer', {method: 'POST', body: JSON.stringify({roundId: result.roundId, tmdbId: movie.tmdbId})});
        if (revision === gameRevision) { renderGame(next, revision); await loadHighscores(); }
      } catch (error) {
        if (revision === gameRevision) {
          $('#game-status').textContent = errorMessage(error) + ' Inicia um novo jogo se a ronda expirou.';
          $('#game-pair').querySelectorAll('button').forEach(b => b.disabled = false);
        }
      }
    });
    $('#game-pair').append(button);
  }
}
