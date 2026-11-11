import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer, validate } from './server.mjs';
const departure = new Date(Date.now() + 86400000 * 30).toISOString().slice(0, 10);
const returning = new Date(Date.now() + 86400000 * 37).toISOString().slice(0, 10);
const query = new URLSearchParams({ origin: 'GRU', destination: 'LIS', departure_at: departure, return_at: returning });
test('validates dates and prevents extra upstream parameters', () => {
  const params = new URLSearchParams(query); params.set('token', 'injected');
  assert.equal(validate(params).get('token'), null);
  for (const [key, value] of [['origin', 'GRU&token=x'], ['return_at', '2026-02-30'], ['destination', 'GRU']]) {
    const invalid = new URLSearchParams(query); invalid.set(key, value); assert.throws(() => validate(invalid));
  }
});
test('forwards token only in header and does not expose it in response', async () => {
  const server = createServer({ token: 'test-private-token', fetcher: async (url, options) => {
    assert.equal(options.headers['X-Access-Token'], 'test-private-token'); assert.ok(!url.includes('test-private-token'));
    return { ok: true, json: async () => ({ success: true, currency: 'brl', data: [{ price: 123, secret: 'test-private-token' }] }) };
  }});
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/flights?${query}`);
    assert.equal(response.status, 200); const body = await response.json();
    assert.equal(body.currency, 'brl'); assert.equal(body.data[0].price, 123);
    assert.ok(!JSON.stringify(body).includes('test-private-token'));
  } finally { await new Promise(resolve => server.close(resolve)); }
});
test('missing token returns unavailable, never invented prices', async () => {
  const server = createServer({token:''});
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  try {const response=await fetch(`http://127.0.0.1:${server.address().port}/flights?${query}`);assert.equal(response.status,503);}
  finally {await new Promise(resolve=>server.close(resolve));}
});
