/**
 * Yatri-Mitra Backend — Pure Node.js, zero dependencies
 * Run: node server.js
 */

const http  = require('http');
const fs    = require('fs');
const path  = require('path');
const crypto = require('crypto');

const PORT      = 3000;
const DB_FILE   = path.join(__dirname, 'users.json');
const JWT_SECRET = 'yatrimitra-secret-2025';

// ── Tiny "database" (JSON file) ───────────────────────────────────────────────
function readUsers() {
    try {
        if (!fs.existsSync(DB_FILE)) fs.writeFileSync(DB_FILE, '[]');
        return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
    } catch { return []; }
}

function saveUsers(users) {
    fs.writeFileSync(DB_FILE, JSON.stringify(users, null, 2));
}

// ── Crypto helpers ────────────────────────────────────────────────────────────
function hashPassword(password) {
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.createHmac('sha256', salt).update(password).digest('hex');
    return `${salt}:${hash}`;
}

function verifyPassword(password, stored) {
    const [salt, hash] = stored.split(':');
    const check = crypto.createHmac('sha256', salt).update(password).digest('hex');
    return check === hash;
}

// ── Minimal JWT (base64 JSON — good enough for local dev) ─────────────────────
function createToken(userId, email) {
    const header  = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64');
    const payload = Buffer.from(JSON.stringify({ userId, email, iat: Date.now() })).toString('base64');
    const sig     = crypto.createHmac('sha256', JWT_SECRET).update(`${header}.${payload}`).digest('base64');
    return `${header}.${payload}.${sig}`;
}

function verifyToken(token) {
    try {
        const [header, payload, sig] = token.split('.');
        const expected = crypto.createHmac('sha256', JWT_SECRET).update(`${header}.${payload}`).digest('base64');
        if (sig !== expected) return null;
        return JSON.parse(Buffer.from(payload, 'base64').toString());
    } catch { return null; }
}

// ── Request helpers ───────────────────────────────────────────────────────────
function readBody(req) {
    return new Promise((resolve) => {
        let data = '';
        req.on('data', chunk => { data += chunk; });
        req.on('end', () => {
            try { resolve(JSON.parse(data || '{}')); }
            catch { resolve({}); }
        });
        req.on('error', () => resolve({}));
    });
}

function send(res, status, obj) {
    const body = JSON.stringify(obj);
    res.writeHead(status, {
        'Content-Type':  'application/json',
        'Content-Length': Buffer.byteLength(body),
        'Access-Control-Allow-Origin':  '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type,Authorization'
    });
    res.end(body);
}

function ok(res, message, data)  { send(res, 200, { success: true,  message, ...data }); }
function err(res, status, message) { send(res, status, { success: false, message }); }

// ── Route handlers ────────────────────────────────────────────────────────────
async function handleRegister(req, res) {
    const { name, email, phone, password } = await readBody(req);

    if (!name || !email || !password)
        return err(res, 400, 'Name, email and password are required');
    if (password.length < 6)
        return err(res, 400, 'Password must be at least 6 characters');

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email))
        return err(res, 400, 'Invalid email address');

    const users = readUsers();
    if (users.find(u => u.email === email.toLowerCase()))
        return err(res, 409, 'An account with this email already exists');

    const user = {
        id:        crypto.randomUUID(),
        name:      name.trim(),
        email:     email.toLowerCase().trim(),
        phone:     (phone || '').trim(),
        password:  hashPassword(password),
        createdAt: new Date().toISOString()
    };
    users.push(user);
    saveUsers(users);

    const token = createToken(user.id, user.email);
    ok(res, 'Registration successful', {
        token,
        user: { id: user.id, name: user.name, email: user.email, phone: user.phone }
    });
}

async function handleLogin(req, res) {
    const { email, password } = await readBody(req);

    if (!email || !password)
        return err(res, 400, 'Email and password are required');

    const users = readUsers();
    const user  = users.find(u => u.email === email.toLowerCase().trim());
    if (!user)
        return err(res, 401, 'No account found with this email');

    if (!verifyPassword(password, user.password))
        return err(res, 401, 'Incorrect password');

    const token = createToken(user.id, user.email);
    ok(res, 'Login successful', {
        token,
        user: { id: user.id, name: user.name, email: user.email, phone: user.phone }
    });
}

async function handleProfile(req, res) {
    const authHeader = req.headers['authorization'] || '';
    const token      = authHeader.replace('Bearer ', '');
    const decoded    = verifyToken(token);
    if (!decoded) return err(res, 401, 'Invalid or expired token');

    const users = readUsers();
    const user  = users.find(u => u.id === decoded.userId);
    if (!user) return err(res, 404, 'User not found');

    ok(res, 'Profile fetched', {
        user: { id: user.id, name: user.name, email: user.email, phone: user.phone }
    });
}

// ── Main server ───────────────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
    // CORS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin':  '*',
            'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type,Authorization'
        });
        return res.end();
    }

    const url = req.url.split('?')[0];
    console.log(`${new Date().toLocaleTimeString()} ${req.method} ${url}`);

    try {
        if (url === '/api/health' && req.method === 'GET')
            return ok(res, 'Yatri-Mitra backend is running!', { port: PORT });

        if (url === '/api/auth/register' && req.method === 'POST')
            return await handleRegister(req, res);

        if (url === '/api/auth/login' && req.method === 'POST')
            return await handleLogin(req, res);

        if (url === '/api/auth/profile' && req.method === 'GET')
            return await handleProfile(req, res);

        err(res, 404, `Route not found: ${req.method} ${url}`);
    } catch (e) {
        console.error('Server error:', e);
        err(res, 500, 'Internal server error');
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log('\n🛺  Yatri-Mitra Backend Running');
    console.log('================================');
    console.log(`   URL  : http://localhost:${PORT}`);
    console.log(`   Test : http://localhost:${PORT}/api/health`);
    console.log(`   DB   : ${DB_FILE}`);
    console.log('================================\n');
    console.log('Waiting for requests...\n');
});

server.on('error', (e) => {
    if (e.code === 'EADDRINUSE')
        console.error(`\n❌ Port ${PORT} is already in use. Stop the other process and retry.\n`);
    else
        console.error('Server error:', e);
});
