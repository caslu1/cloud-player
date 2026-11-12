-- Banco do site de ativação
CREATE TABLE IF NOT EXISTS devices (
  mac TEXT PRIMARY KEY,
  dkey TEXT NOT NULL,
  platform TEXT DEFAULT '',
  model TEXT DEFAULT '',
  created_at INTEGER NOT NULL,
  trial_until INTEGER NOT NULL,
  plan TEXT DEFAULT 'trial',          -- trial | annual | lifetime
  expires_at INTEGER,                 -- fim do plano anual (segundos)
  blocked INTEGER DEFAULT 0,
  last_seen INTEGER,
  note TEXT DEFAULT ''
);
CREATE TABLE IF NOT EXISTS playlists (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mac TEXT NOT NULL,
  name TEXT NOT NULL,
  type TEXT NOT NULL,                 -- xtream | m3u
  server TEXT DEFAULT '',
  username TEXT DEFAULT '',
  password TEXT DEFAULT '',
  url TEXT DEFAULT '',
  pin TEXT DEFAULT '',
  position INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pl_mac ON playlists(mac);
CREATE TABLE IF NOT EXISTS payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mac TEXT NOT NULL,
  plan TEXT NOT NULL,
  amount REAL NOT NULL,
  mp_id TEXT,
  status TEXT NOT NULL DEFAULT 'pending',   -- pending | approved | cancelled | expired
  qr TEXT,
  qr_b64 TEXT,
  created_at INTEGER NOT NULL,
  paid_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_pay_mac ON payments(mac);
CREATE TABLE IF NOT EXISTS settings (k TEXT PRIMARY KEY, v TEXT NOT NULL);
