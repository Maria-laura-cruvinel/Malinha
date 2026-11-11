import http from 'node:http';
import { pathToFileURL } from 'node:url';

// Only this fixed, read-only upstream endpoint is exposed. No account endpoints.
export function validate(input) {
  const origin = (input.get('origin') || '').trim().toUpperCase();
  const destination = (input.get('destination') || '').trim().toUpperCase();
  const departure = input.get('departure_at') || '';
  const returning = input.get('return_at') || '';
  const validDate = value => /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value;
  if (!/^[A-Z]{3}$/.test(origin) || !/^[A-Z]{3}$/.test(destination) || origin === destination ||
      !validDate(departure) || !validDate(returning) || returning < departure ||
      departure < new Date().toISOString().slice(0, 10)) throw new Error('invalid_query');
  return new URLSearchParams({ origin, destination, departure_at: departure, return_at: returning,
    one_way: 'false', currency: 'brl', sorting: 'price', direct: 'false', unique: 'false', limit: '20', page: '1' });
}

export function createServer({ token = process.env.TRAVELPAYOUTS_TOKEN, fetcher = fetch } = {}) {
  let active = 0;
  const recent = new Map();
  return http.createServer(async (req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(body));
    };
    const url = new URL(req.url, 'http://localhost');
    if (req.method !== 'GET' || url.pathname !== '/flights') return send(404, { error: 'not_found' });
    let query;
    try { query = validate(url.searchParams); } catch { return send(400, { error: 'invalid_query' }); }
    if (!token) return send(503, { error: 'not_configured' });
    const now = Date.now(), key = req.socket.remoteAddress;
    for (const [ip, entry] of recent) if (now - entry.start > 60000) recent.delete(ip);
    const bucket = recent.get(key) || { start: now, count: 0 }; bucket.count++; recent.set(key, bucket);
    if (bucket.count > 20 || active >= 4) return send(429, { error: 'rate_limited' });
    active++;
    try {
      const response = await fetcher('https://api.travelpayouts.com/aviasales/v3/prices_for_dates?' + query,
        { headers: { 'X-Access-Token': token, Accept: 'application/json' }, signal: AbortSignal.timeout(15000), redirect: 'error' });
      if (!response.ok) return send(response.status === 429 ? 429 : 502, { error: 'upstream_unavailable' });
      const body = await response.json();
      if (body.success !== true || !Array.isArray(body.data) || String(body.currency).toLowerCase() !== 'brl')
        return send(502, { error: 'invalid_response' });
      // Allowlist fields; never forward upstream errors, credentials or arbitrary URLs.
      const data = body.data.slice(0, 20).map(row => Object.fromEntries(
        ['origin', 'destination', 'origin_airport', 'destination_airport', 'price', 'airline', 'flight_number', 'departure_at', 'return_at', 'transfers', 'return_transfers', 'link']
          .filter(field => row[field] !== undefined).map(field => [field, row[field]])));
      send(200, { success: true, currency: 'brl', queried_at: new Date().toISOString(), data });
    } catch { send(502, { error: 'upstream_unavailable' }); }
    finally { active--; }
  });
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const port = Number(process.env.PORT || 8787);
  createServer().listen(port, '127.0.0.1', () => console.log(`Flight proxy listening on localhost:${port}`));
}
