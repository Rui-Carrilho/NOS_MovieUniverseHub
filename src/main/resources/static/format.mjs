export function formatRating(rating, count) {
  if (count === 0) return 'Sem votos';
  if (rating == null || count == null) return 'Nota indisponível';
  const score = new Intl.NumberFormat('pt-PT', {minimumFractionDigits: 1, maximumFractionDigits: 1}).format(rating);
  return `${score} · ${new Intl.NumberFormat('pt-PT').format(count)} votos`;
}

export function releaseYear(date) {
  return typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date.slice(0, 4) : 'Ano indisponível';
}

export function posterUrl(value) {
  if (typeof value !== 'string') return null;
  try {
    const url = new URL(value);
    return url.origin === 'https://image.tmdb.org' && url.pathname.startsWith('/t/p/') ? url.href : null;
  } catch { return null; }
}
