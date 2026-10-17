/** CASA CONECTADA - API local de pruebas (NO desplegar todavia).
 * Cuenta propia, inicio de sesion, invitaciones y perfil.
 * La integracion con Android, correo verificado y proteccion antiabuso son fases posteriores.
 */
interface Env {
  DB: D1Database;
  SETUP_KEY: string;
}
type Obj = Record<string, unknown>;
type Identity = { userId: string; email: string; tokenHash: string };
const encoder = new TextEncoder();
const ITERATIONS = 150_000;
const SESSION_MS = 7 * 24 * 60 * 60 * 1000;
const INVITE_MS = 24 * 60 * 60 * 1000;

function reply(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', 'x-content-type-options': 'nosniff' }
  });
}
function fail(status: number, code: string): Response { return reply({ error: code }, status); }
function iso(time = Date.now()): string { return new Date(time).toISOString(); }
function hex(bytes: Uint8Array): string { return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join(''); }
function randomHex(size = 32): string { const a = new Uint8Array(size); crypto.getRandomValues(a); return hex(a); }
async function sha(text: string): Promise<string> {
  return hex(new Uint8Array(await crypto.subtle.digest('SHA-256', encoder.encode(text))));
}
function same(a: string, b: string): boolean {
  const x = encoder.encode(a), y = encoder.encode(b);
  let mismatch = x.length ^ y.length;
  for (let i = 0; i < Math.max(x.length, y.length); i++) mismatch |= (x[i] ?? 0) ^ (y[i] ?? 0);
  return mismatch === 0;
}
async function hashPassword(password: string, salt = randomHex(16)): Promise<string> {
  const imported = await crypto.subtle.importKey('raw', encoder.encode(password), 'PBKDF2', false, ['deriveBits']);
  const key = await crypto.subtle.deriveBits({ name: 'PBKDF2', salt: encoder.encode(salt), iterations: ITERATIONS, hash: 'SHA-256' }, imported, 256);
  return `pbkdf2-sha256$${ITERATIONS}$${salt}$${hex(new Uint8Array(key))}`;
}
async function verifyPassword(password: string, stored: string | null): Promise<boolean> {
  if (!stored) return false;
  const p = stored.split('$');
  if (p.length !== 4 || p[0] !== 'pbkdf2-sha256' || p[1] !== String(ITERATIONS) || !/^[a-f0-9]{32}$/.test(p[2])) return false;
  return same((await hashPassword(password, p[2])).split('$')[3], p[3]);
}
async function body(request: Request): Promise<Obj | null> {
  if (!request.headers.get('content-type')?.toLowerCase().includes('application/json')) return null;
  if (Number(request.headers.get('content-length') ?? 0) > 8192) return null;
  const text = await request.text();
  if (text.length > 8192) return null;
  try {
    const obj: unknown = JSON.parse(text);
    return (obj && typeof obj === 'object' && !Array.isArray(obj)) ? obj as Obj : null;
  } catch { return null; }
}
function str(v: unknown, max: number): string | null {
  if (typeof v !== 'string') return null;
  const result = v.trim();
  return result && result.length <= max ? result : null;
}
function email(v: unknown): string | null {
  const e = str(v, 254)?.toLowerCase();
  return e && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e) ? e : null;
}
function password(v: unknown): string | null {
  return typeof v === 'string' && v.length >= 12 && v.length <= 128 ? v : null;
}
async function sessionStatements(env: Env, userId: string) {
  const token = randomHex();
  const expires = iso(Date.now() + SESSION_MS);
  const statement = env.DB.prepare('INSERT INTO auth_sessions (id,user_id,token_hash,expires_at) VALUES (?,?,?,?)')
    .bind(crypto.randomUUID(), userId, await sha(token), expires);
  return { statement, token, expires };
}
async function authorize(request: Request, env: Env): Promise<Identity | null> {
  const header = request.headers.get('authorization') ?? '';
  const m = /^Bearer ([a-f0-9]{64})$/.exec(header);
  if (!m) return null;
  const tokenHash = await sha(m[1]);
  const row = await env.DB.prepare(`SELECT u.id AS userId,u.email_normalized AS email
    FROM auth_sessions s JOIN users u ON u.id=s.user_id
    WHERE s.token_hash=? AND s.expires_at > ? AND u.status='ACTIVE'`)
    .bind(tokenHash, iso()).first<{ userId: string; email: string }>();
  return row ? { ...row, tokenHash } : null;
}
async function adminInFamily(env: Env, userId: string, familyId: string): Promise<boolean> {
  return Boolean(await env.DB.prepare("SELECT 1 FROM family_members WHERE user_id=? AND family_id=? AND role='ADMIN'")
    .bind(userId, familyId).first());
}
async function memberInFamily(env: Env, userId: string, familyId: string): Promise<boolean> {
  return Boolean(await env.DB.prepare('SELECT 1 FROM family_members WHERE user_id=? AND family_id=?')
    .bind(userId, familyId).first());
}
async function handler(request: Request, env: Env): Promise<Response> {
  const u = new URL(request.url);
  const p = u.pathname.replace(/\/$/, '') || '/';
  if (request.method === 'GET' && p === '/health') return reply({ status: 'ok', app: 'casa-conectada-api', mode: 'local-testing' });
  if (request.method === 'POST' && p === '/api/v1/setup/admin') {
    if (!env.SETUP_KEY || env.SETUP_KEY.length < 32) return fail(503, 'setup_not_configured');
    const key = request.headers.get('x-setup-key') ?? '';
    if (!same(key, env.SETUP_KEY)) return fail(403, 'setup_denied');
    const b = await body(request);
    const e = email(b?.email), name = str(b?.displayName, 80), family = str(b?.familyName, 80), pass = password(b?.password);
    if (!e || !name || !family || !pass) return fail(400, 'invalid_input');
    if (await env.DB.prepare('SELECT 1 FROM setup_guard WHERE id=1').first()) return fail(409, 'setup_completed');
    const id = crypto.randomUUID(), familyId = crypto.randomUUID();
    const hash = await hashPassword(pass);
    const s = await sessionStatements(env, id);
    try {
      await env.DB.batch([
        env.DB.prepare('INSERT INTO setup_guard (id) VALUES (1)'),
        env.DB.prepare("INSERT INTO users (id,email_normalized,password_hash,status) VALUES (?,?,?,'ACTIVE')").bind(id, e, hash),
        env.DB.prepare('INSERT INTO user_profiles (user_id,display_name) VALUES (?,?)').bind(id, name),
        env.DB.prepare('INSERT INTO families (id,name,created_by_user_id) VALUES (?,?,?)').bind(familyId, family, id),
        env.DB.prepare("INSERT INTO family_members (family_id,user_id,role) VALUES (?,?,'ADMIN')").bind(familyId, id),
        s.statement
      ]);
    } catch { return fail(409, 'setup_could_not_complete'); }
    return reply({ userId: id, familyId, role: 'ADMIN', accessToken: s.token, expiresAt: s.expires }, 201);
  }
  if (request.method === 'POST' && p === '/api/v1/auth/login') {
    const b = await body(request); const e = email(b?.email), pass = password(b?.password);
    if (!e || !pass) return fail(400, 'invalid_input');
    const user = await env.DB.prepare('SELECT id,password_hash FROM users WHERE email_normalized=? AND status=\'ACTIVE\'')
      .bind(e).first<{ id: string; password_hash: string | null }>();
    // En produccion se anadira proteccion anti fuerza bruta, email verificado y limites por IP.
    if (!user || !await verifyPassword(pass, user.password_hash)) return fail(401, 'invalid_credentials');
    const s = await sessionStatements(env, user.id);
    await s.statement.run();
    return reply({ accessToken: s.token, expiresAt: s.expires });
  }
  if (request.method === 'POST' && p === '/api/v1/auth/register-invite') {
    const b = await body(request); const code = str(b?.invitationToken, 128);
    const e = email(b?.email), name = str(b?.displayName, 80), pass = password(b?.password);
    if (!code || !e || !name || !pass) return fail(400, 'invalid_input');
    const invite = await env.DB.prepare(`SELECT id,family_id,invited_email_normalized
      FROM invitations WHERE token_hash=? AND accepted_at IS NULL AND revoked_at IS NULL AND expires_at > ? AND role='MEMBER'`)
      .bind(await sha(code), iso()).first<{ id: string; family_id: string; invited_email_normalized: string | null }>();
    if (!invite || (invite.invited_email_normalized && invite.invited_email_normalized.toLowerCase() !== e))
      return fail(403, 'invitation_invalid');
    const id = crypto.randomUUID(), hash = await hashPassword(pass), s = await sessionStatements(env, id);
    try {
      await env.DB.batch([
        env.DB.prepare("INSERT INTO users (id,email_normalized,password_hash,status) VALUES (?,?,?,'ACTIVE')").bind(id, e, hash),
        env.DB.prepare('INSERT INTO user_profiles (user_id,display_name) VALUES (?,?)').bind(id, name),
        env.DB.prepare('INSERT INTO invitation_claims (invitation_id,user_id) VALUES (?,?)').bind(invite.id, id),
        env.DB.prepare("INSERT INTO family_members (family_id,user_id,role) VALUES (?,?,'MEMBER')").bind(invite.family_id, id),
        env.DB.prepare('UPDATE invitations SET accepted_at=?,accepted_by_user_id=? WHERE id=?').bind(iso(), id, invite.id),
        s.statement
      ]);
    } catch { return fail(409, 'registration_failed_or_invitation_used'); }
    return reply({ userId: id, familyId: invite.family_id, role: 'MEMBER', accessToken: s.token, expiresAt: s.expires }, 201);
  }
  const identity = await authorize(request, env);
  if (!identity) return fail(401, 'authentication_required');
  if (request.method === 'GET' && p === '/api/v1/me') {
    const profile = await env.DB.prepare(`SELECT u.id, u.email_normalized AS email, p.display_name AS displayName,
      p.username, p.phone, p.bio FROM users u JOIN user_profiles p ON p.user_id=u.id WHERE u.id=?`)
      .bind(identity.userId).first();
    const families = await env.DB.prepare(`SELECT f.id,f.name,m.role FROM family_members m
      JOIN families f ON f.id=m.family_id WHERE m.user_id=? ORDER BY f.name`)
      .bind(identity.userId).all();
    return reply({ profile, families: families.results });
  }
  if (request.method === 'PATCH' && p === '/api/v1/me') {
    const b = await body(request), name = str(b?.displayName, 80);
    if (!name) return fail(400, 'invalid_display_name');
    const username = b?.username === null || b?.username === '' ? null : str(b?.username, 40);
    const phone = b?.phone === null || b?.phone === '' ? null : str(b?.phone, 30);
    const bio = b?.bio === null || b?.bio === '' ? null : str(b?.bio, 300);
    if ((b?.username != null && b.username !== '' && !username) || (b?.phone != null && b.phone !== '' && !phone)
      || (b?.bio != null && b.bio !== '' && !bio)) return fail(400, 'invalid_input');
    try {
      await env.DB.prepare(`UPDATE user_profiles SET display_name=?,username=?,phone=?,bio=?,updated_at=? WHERE user_id=?`)
        .bind(name, username, phone, bio, iso(), identity.userId).run();
    } catch { return fail(409, 'profile_conflict'); }
    return reply({ updated: true });
  }
  if (request.method === 'POST' && p === '/api/v1/auth/logout') {
    await env.DB.prepare('DELETE FROM auth_sessions WHERE token_hash=?').bind(identity.tokenHash).run();
    return reply({ loggedOut: true });
  }
  const match = /^\/api\/v1\/families\/([a-z0-9-]+)\/(invitations|members)$/.exec(p);
  if (match) {
    const familyId = match[1], resource = match[2];
    if (!await memberInFamily(env, identity.userId, familyId)) return fail(403, 'family_access_denied');
    if (request.method === 'GET' && resource === 'members') {
      const result = await env.DB.prepare(`SELECT m.user_id AS userId,p.display_name AS displayName,m.role
        FROM family_members m JOIN user_profiles p ON p.user_id=m.user_id WHERE m.family_id=? ORDER BY m.role,m.joined_at`)
        .bind(familyId).all();
      return reply({ members: result.results });
    }
    if (request.method === 'POST' && resource === 'invitations') {
      if (!await adminInFamily(env, identity.userId, familyId)) return fail(403, 'admin_only');
      const b = await body(request), invitedEmail = email(b?.email);
      if (!invitedEmail) return fail(400, 'invalid_email');
      const code = randomHex();
      const expiration = iso(Date.now() + INVITE_MS);
      await env.DB.prepare(`INSERT INTO invitations
        (id,family_id,invited_email_normalized,token_hash,role,created_by_user_id,expires_at)
        VALUES (?,?,?,?,'MEMBER',?,?)`)
        .bind(crypto.randomUUID(), familyId, invitedEmail, await sha(code), identity.userId, expiration).run();
      return reply({ invitationToken: code, invitedEmail, expiresAt: expiration, note: 'Comparte el codigo por un medio privado. Aun no se envia correo.' }, 201);
    }
  }
  return fail(404, 'not_found');
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try { return await handler(request, env); }
    catch (error) {
      console.error('API unexpected error:', error instanceof Error ? error.name : 'unknown');
      return fail(500, 'server_error');
    }
  }
} satisfies ExportedHandler<Env>;
