# Authentication Flow

How authentication works in `springboot-setup-heakcg`: stateless JWT (HS256) issued on
register/login, with every issued access token also recorded in the database so it can be revoked.

Because `server.servlet.context-path: /api`, every URL below is prefixed with `/api` in real
requests (`/api/auth/login`, `/api/user/all`, …). Path patterns inside the security config are
matched *after* the context path is stripped.

---

## 1. The components

| Component | Role in auth |
|---|---|
| `configs/ApplicationConfig` | Publishes four beans: `PasswordEncoder` (BCrypt), `UserDetailsService` (lambda → `userRepository.findByEmail`), `AuthenticationProvider` (`DaoAuthenticationProvider` wired with those two), `AuthenticationManager` (`ProviderManager` wrapping the provider) |
| `security/SecurityConfiguration` | The `SecurityFilterChain`: CSRF disabled, `STATELESS` sessions, URL authorization rules, and registration of the JWT filter |
| `security/JwtAuthenticationFilter` | Runs once per request; turns an `Authorization: Bearer …` header into an authenticated `SecurityContext` |
| `services/JwtService` | Mints and parses/verifies HS256 tokens using the secret and TTLs from `application.yaml` |
| `controllers/auth/AuthenticateController` | The `/auth/register` and `/auth/login` endpoints |
| `entities/User` | Implements `UserDetails` directly — the JPA row *is* the security principal. `getUsername()` returns the **email** |
| `entities/Token` | One row per issued access token, with `revoked` / `expired` flags |
| `enums/Role`, `enums/Permission` | `Role.getAuthorities()` returns the fine-grained permissions (`admin:read`, …) **plus** `ROLE_<NAME>` |

Relevant configuration in `src/main/resources/application.yaml`:

```yaml
application:
  security:
    jwt:
      secret-key: <base64 HMAC key>
      expiration: 86400000          # access token — 24h
      refresh-token:
        expiration: 604800000       # refresh token — 7d
```

---

## 2. Register — `POST /api/auth/register`

Form-encoded params: `name`, `email`, `password`, `role`.

1. Spring binds the body into a `User` via `@ModelAttribute`.
2. The request is public: `SecurityConfiguration` permits `/auth/**`, and `JwtAuthenticationFilter`
   short-circuits because the servlet path contains `/auth`. No token needed.
3. `userRepository.findByEmail(...)` — if the email is taken, the endpoint returns **200** with
   `{"message": "User has existed"}` (not a 409).
4. Otherwise the user is rebuilt with `passwordEncoder.encode(password)`; the **BCrypt hash** is what
   gets persisted, never the raw password.
5. `jwtService.generateToken(user)` builds an HS256 JWT whose **subject is the email**, with `iat`
   and `exp = now + application.security.jwt.expiration`.
6. `saveUserToken(...)` inserts a `Token` row (`BEARER`, `revoked=false`, `expired=false`) linked to
   the user.
7. Response: `message`, `access_token`, `user`.

```
client → /auth/register → (filter skipped) → controller
    → findByEmail → encode(password) → save(user)
    → generateToken → save(Token row) → 200 {message, access_token, user}
```

---

## 3. Login — `POST /api/auth/login`

Form-encoded params: `email`, `password`.

1. `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`
   → `ProviderManager` → `DaoAuthenticationProvider`, which calls
   `UserDetailsService.loadUserByUsername(email)` and compares the raw password against the stored
   BCrypt hash.
   **Unknown email or wrong password throws here** — Spring Security returns 401/403 and the rest of
   the method never runs.
2. The controller reloads the `User` and mints two tokens: an **access token** (24h) and a
   **refresh token** (7d).
3. `revokeAllUserTokens(user)` sets `expired=true, revoked=true` on every currently-valid token row
   for that user — older access tokens stop working immediately.
4. `saveUserToken(...)` inserts the new access token row.
5. Response: `message`, `user`, `access_token`, `refresh_token`.

The `SecurityContext` created by `authenticate()` is **not** persisted — sessions are `STATELESS`,
so the returned JWT is the only thing that carries identity into later requests.

```
client → /auth/login → (filter skipped) → controller
    → authenticationManager.authenticate  ──✗ bad creds → 401/403
    → generate access + refresh tokens
    → revoke all previous valid tokens → save new Token row
    → 200 {message, user, access_token, refresh_token}
```

---

## 4. Protected request — e.g. `GET /api/user/all`

Header: `Authorization: Bearer <access_token>`

Filter order:

```
request → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → … → authorization rules → controller
```

Inside `JwtAuthenticationFilter.doFilterInternal`:

1. If the servlet path contains `/auth`, pass straight through — auth endpoints are never token-checked.
2. If there is no `Authorization` header, or it does not start with `Bearer `, pass through
   **unauthenticated**; the authorization rules reject it later (403).
3. Strip the `Bearer ` prefix and call `jwtService.extractUsername(jwt)`. This parses **and
   cryptographically verifies** the signature against the HMAC key — a tampered token or one signed
   with a different key throws here.
4. If a username came out and nothing is authenticated yet, load `UserDetails` from the database by
   email.
5. **Two-part validity check**, both must pass:
   - the token exists in the `token` table and is neither `expired` nor `revoked`
     (`tokenRepository.findByToken`), and
   - `jwtService.isTokenValid` confirms the subject matches the loaded user and `exp` has not passed.

   The DB half is what makes revocation possible at all with otherwise-stateless JWTs.
6. On success, an authenticated `UsernamePasswordAuthenticationToken` carrying
   `userDetails.getAuthorities()` is placed in the `SecurityContextHolder`.
7. Authorization rules then apply:
   - `/` and `/auth/**` → public
   - `/user/**` → `hasRole("ADMIN")`, which matches the `ROLE_ADMIN` authority appended by
     `Role.getAuthorities()`
   - everything else → any authenticated user
8. The `SecurityContext` is cleared when the request ends.

---

## 5. Where state lives

| State | Location | Lifetime |
|---|---|---|
| Credentials | `users` table, BCrypt hash | Until changed |
| Access token | JWT held by the client **and** a row in `token` | 24h, or until a new login revokes it |
| Refresh token | Returned to the client only — **never stored** | 7d (but unusable, see below) |
| Session | None — `SessionCreationPolicy.STATELESS` | Per request |

---

## 6. Known gaps

Things this flow does **not** do. Worth knowing before building on it.

- **Registration accepts `role` from the client.** Anyone can register as `ADMIN` and immediately
  pass the `/user/**` rule. This is the first thing to fix — force `Role.USER` server-side.
- **The `User` entity is serialized into responses**, and it has a public `getPassword()`, so the
  BCrypt hash ships to the client in the register/login JSON. Add `@JsonIgnore` or return a DTO.
- **No `/refresh` endpoint, and the refresh token is never saved.** Since the filter requires a
  matching DB row, the refresh token cannot authenticate anything — it is returned and then dead.
  Re-login is currently the only way to renew.
- **No logout endpoint**, even though `revoked` / `expired` exist for exactly that purpose.
  Revocation only happens as a side effect of a new login.
- **A malformed or bad-signature token throws from the filter** and surfaces as a 500 rather than a
  clean 401. Wrap the parse in a try/catch and delegate to an `AuthenticationEntryPoint`.
- **Revoked and expired token rows are never purged** — the `token` table grows unbounded.
- `types/RoleType` is dead code; the real role enum is `enums/Role`.
