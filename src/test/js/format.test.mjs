import test from 'node:test';
import assert from 'node:assert/strict';
import {formatRating, releaseYear, posterUrl} from '../../main/resources/static/format.mjs';

test('zero votes is never a zero rating', () => {
  assert.equal(formatRating(0, 0), 'Sem votos');
  assert.equal(formatRating(null, 0), 'Sem votos');
  assert.match(formatRating(8.4, 30000), /8,4.*votos/);
});
test('missing release year has explicit fallback', () => {
  assert.equal(releaseYear(null), 'Ano indisponível');
  assert.equal(releaseYear(''), 'Ano indisponível');
  assert.equal(releaseYear('2020-01-01'), '2020');
});
test('images are restricted to the TMDB HTTPS host', () => {
  assert.equal(posterUrl('javascript:alert(1)'), null);
  assert.equal(posterUrl('https://example.com/poster.jpg'), null);
  assert.equal(posterUrl('https://image.tmdb.org/t/p/w500/a.jpg'), 'https://image.tmdb.org/t/p/w500/a.jpg');
});
