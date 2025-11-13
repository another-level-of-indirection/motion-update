## Motion-updated

Update of https://github.com/chr15m/motion with latest react, shadow-cljs, reagent.

### Prerequisites

- Java JDK 11+
- Node.js 18+
- pnpm 9+ (recommended) or npm/yarn
- Clojure CLI (tools.deps) — required because `shadow-cljs.edn` uses `deps.edn`

Verify your tools:

```bash
java -version
node -v
pnpm -v        # or npm -v / yarn -v
clojure -M -e '(println :ok)'
```

### Install

```bash
pnpm install
```

If you prefer npm:

```bash
npm install
```

### Run (development)

Start the Shadow CLJS watcher and built‑in dev HTTP server:

```bash
pnpm run dev
```

Then open:

- http://localhost:8080

Notes:

- Hot reloading is enabled (`motion.dev/reload`).
- Static assets are served from `resources/public` per `:dev-http {8080 "resources/public"}`.

Optional: connect a ClojureScript REPL (after the watcher is running):

```bash
pnpm run repl
```

### Build (production)

Produce an optimized build to `build/js` and assemble a static `build/` directory:

```bash
# 1) Copy static assets
rm -rf build && mkdir -p build && cp -R resources/public/* build/

# 2) Compile optimized ClojureScript (outputs to build/js)
pnpm run release
```

Serve the `build/` folder with any static file server, for example:

```bash
npx serve build -l 8080
```

### Common issues

- Port 8080 in use: change `:dev-http {8080 "resources/public"}` in `shadow-cljs.edn` or stop the conflicting process.
- Missing Java or Clojure CLI: install them and re‑run the commands above.
- REPL cannot connect: ensure `pnpm run dev` is active before `pnpm run repl`.

### Useful scripts

From `package.json`:

- `pnpm run dev` — `shadow-cljs watch app`
- `pnpm run compile` — one‑off compile of the `app` build (unoptimized)
- `pnpm run release` — optimized `release` build to `build/js`
- `pnpm run repl` — connect a CLJS REPL to the `app` build

### Project layout

- `src/cljs` — application UI code
- `env/dev/cljs` — development entrypoint (`motion.dev`) with hot reload
- `env/prod/cljs` — production entrypoint (`motion.prod`)
- `resources/public` — static assets and `index.html` (dev server root)
- `build/js` — production JS output (after `pnpm run release`)
