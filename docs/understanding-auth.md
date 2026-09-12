# Understanding Any Authentication Implementation

A tactic for reading unfamiliar auth code without drowning in it.

There are many authentication implementations, but they are all the same four slots. They only look
different because each one fills the slots differently. Learn the slots, then read any codebase by
filling in a table.

---

## 1. The four slots

```
     ISSUE                                 VERIFY
  ┌──────────────────────┐            ┌──────────────────────────┐
  │ 1. Prove identity    │            │ 3. Read the credential   │
  │    once (login)      │  ──────→   │    on every request      │
  │                      │  client    │                          │
  │ 2. Hand back a       │  carries   │ 4. Load identity +       │
  │    portable          │  it        │    authorities → decide  │
  │    credential        │            │    allow/deny            │
  └──────────────────────┘            └──────────────────────────┘
```

1. **Where are credentials checked?** (password vs. stored hash, or an external identity provider)
2. **What does the client carry afterward?** (session id, JWT, API key, opaque token)
3. **Who reads that on each request, and where in the pipeline?**
4. **Where do identity and permissions come from, and who decides allow/deny?**

That is the whole model. Everything else is packaging.

---

## 2. This project, in the four slots

| Slot | Where it lives here |
|---|---|
| 1. Credential check | `AuthenticationManager` → `DaoAuthenticationProvider` → BCrypt compare, triggered in `/auth/login` |
| 2. What the client carries | A signed JWT (subject = email), plus a mirror row in the `token` table |
| 3. Who reads it | `JwtAuthenticationFilter`, inserted before `UsernamePasswordAuthenticationFilter` |
| 4. Identity + decision | `UserDetailsService` loads `User` → `Role.getAuthorities()` → the `hasRole("ADMIN")` rule |

Full detail on this specific flow: [authentication-flow.md](authentication-flow.md).

---

## 3. The same table for the variants

| | Slot 1: check | Slot 2: carries | Slot 3: reader | Slot 4: identity from |
|---|---|---|---|---|
| **Classic session** | login form | `JSESSIONID` cookie | `SecurityContextPersistenceFilter` | server-side session store |
| **Stateless JWT** | login form | JWT only | your own filter | the token's own claims |
| **JWT + DB** (this project) | login form | JWT | your own filter | DB, and the token row is checked too |
| **OAuth2 / OIDC** | *someone else's server* | JWT or opaque token | `BearerTokenAuthenticationFilter` | JWKS keys or an introspection endpoint |
| **API key** | none | static key header | custom filter | key → tenant lookup |
| **HTTP Basic** | on every request | base64 header | `BasicAuthenticationFilter` | `UserDetailsService` |

Read the columns, not the rows. Slots 1 and 4 barely change between implementations. Nearly all the
apparent variety is **slot 2** — what the client carries — and that reduces to a single question:

> **Can the server verify this credential without looking anything up?**
>
> - **No** → stateful (session id, opaque token). Easy to revoke, needs shared server-side state.
> - **Yes** → stateless (signed JWT). Cheap to verify and scales across servers, but you cannot take
>   it back before it expires.

This project sits deliberately in between: the JWT is self-verifying, but the filter *also* checks a
DB row, giving up some statelessness to buy revocation. Every design you read is somewhere on that
one tradeoff line.

---

## 4. The reading recipe

Works on any Spring Security codebase in about five minutes. Do these in order and stop when the
picture closes.

**Step 0 — find the map:**

```bash
grep -rn "SecurityFilterChain\|WebSecurityConfigurerAdapter" src/main/java
```

1. **Read the filter chain config.** This is the map of the whole system. Three things matter:
   - `authorizeHttpRequests` → the rules (which paths are public, which need what)
   - `sessionManagement` → `STATELESS` or not, which tells you slot 2 immediately
   - `addFilterBefore` / `addFilterAt` → a custom slot 3 exists
2. **Read slot 3.** Either a class extending `OncePerRequestFilter`, or a built-in filter implied by
   `.httpBasic()` / `.formLogin()` / `.oauth2ResourceServer()`. A custom one is usually under 60
   lines, and it is the heart of the system. Read it top to bottom.
3. **Find slot 4.** `grep -rn "UserDetailsService\|GrantedAuthority"`. See which authorities get
   attached to the principal, then match them against the rules you read in step 1. If `hasRole("X")`
   never matches an authority anyone actually receives, you have found a bug.
4. **Find slot 1.** The login endpoint, or an `AuthenticationProvider` — or *nothing at all*, if this
   is OAuth2 and another server does the checking.
5. **Ask the two follow-up questions:**
   - How does a credential get **revoked**?
   - How does it get **renewed**?

   Tutorial codebases often answer "it doesn't" (this one has no working renewal). Spotting that gap
   is how you tell demo code from production code.

---

## 5. The one sentence to keep

> Authentication is: **prove once, carry something, check the something on every request.**
> Implementations differ only in *what you carry* and *how much the server must remember in order to
> trust it.*

When you open unfamiliar auth code, do not read it linearly. Fill the four slots. If a piece of code
does not belong to a slot, it is not authentication — it is authorization, user CRUD, or error
handling, and you can safely ignore it until the four slots are filled.
