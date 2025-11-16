# Motion5

Science fiction user interfaces implemented in ClojureScript, React, and SVG.

## Pure ClojureScript App

This is a pure ClojureScript application. The **compiled output** has no Java runtime dependencies - it's pure JavaScript that runs in the browser.

### Important Note on Build Tools

While the app itself is pure JavaScript, **Shadow CLJS (the build tool) requires Java** to compile ClojureScript to JavaScript. This is similar to how TypeScript requires Node.js to compile, even though the output is pure JavaScript.

- ✅ **App runtime**: Pure JavaScript, no Java needed
- ⚠️ **Build process**: Requires Java (via Shadow CLJS)

## Prerequisites

- [Node.js](https://nodejs.org/) (v16 or higher recommended)
- [Java JDK](https://adoptium.net/) (version 11 or higher) - required for Shadow CLJS build tool
- [pnpm](https://pnpm.io/) (or npm/yarn)

**Note:** You do NOT need Clojure CLI tools (`clj`/`clojure`). All dependencies are managed by Shadow CLJS directly.

## Getting Started

1. **Install dependencies:**
   ```bash
   pnpm install
   # or: npm install
   ```

2. **Start the development server:**
   ```bash
   pnpm dev
   # or: npm run dev
   ```

   This will:
   - Start Shadow CLJS watch mode
   - Compile ClojureScript to JavaScript
   - Serve the app on http://localhost:8080
   - Auto-reload on code changes

3. **Open your browser:**
   ```
   http://localhost:8080
   ```

## Available Scripts

- `pnpm dev` - Start development server with hot reload
- `pnpm compile` - Compile the app once (no watch)
- `pnpm release` - Create an optimized production build
- `pnpm repl` - Start a ClojureScript REPL connected to the browser

## Project Structure

```
motion5/
├── src/cljs/motion/        # ClojureScript source files
│   ├── core.cljs           # Main application entry
│   ├── components.cljs     # UI components
│   ├── demo_*.cljs         # Various demo animations
│   └── ...
├── resources/public/       # Static assets
│   ├── index.html          # HTML entry point
│   ├── css/                # Stylesheets
│   └── js/                 # Compiled JavaScript (generated)
├── shadow-cljs.edn         # Shadow CLJS configuration
└── package.json            # Node dependencies
```

## Development

The app uses:
- **Shadow CLJS** for building and development
- **Reagent** for React bindings
- **Reitit** for routing
- **SVG** for graphics and animations

All demos are written in pure ClojureScript with no backend required. Shadow CLJS's built-in HTTP server serves the static files during development.

## Production Build

To create an optimized production build:

```bash
pnpm release
```

The build output will be in the `docs/` directory, ready for GitHub Pages deployment.

## Deploying to GitHub Pages

This project is configured for easy deployment to GitHub Pages.

### One-Command Deployment

```bash
pnpm gh-pages
```

This command will:
1. Build an optimized production release
2. Copy all static assets (CSS, fonts, JavaScript) to the `docs/` folder
3. Prepare everything for GitHub Pages

### Setting Up GitHub Pages

After running the deploy command:

1. **Commit and push the changes:**
   ```bash
   git add docs/
   git commit -m "Deploy to GitHub Pages"
   git push origin main
   ```

2. **Enable GitHub Pages in your repository:**
   - Go to your repository on GitHub
   - Click **Settings** → **Pages**
   - Under **Source**, select:
     - Branch: `main`
     - Folder: `/docs`
   - Click **Save**

3. **Access your app:**
   - Your app will be available at: `https://[username].github.io/[repository-name]/`
   - It may take a few minutes for GitHub to deploy

### Updating the Deployment

Whenever you make changes:

```bash
pnpm gh-pages
git add docs/
git commit -m "Update deployment"
git push
```

GitHub Pages will automatically update your site.

## Notes

- The built-in development server on port 8080 serves the application
- Hot reload works automatically in development mode
- You may see Java warnings from Shadow CLJS during compilation - these are from the build tool itself and don't affect the output

### About Java Warnings

When running `pnpm dev`, you might see warnings like:
```
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
```

These warnings come from Shadow CLJS's JVM dependencies and are harmless. They don't affect:
- Your application code
- The compiled JavaScript output
- Runtime performance

The compiled app runs as pure JavaScript in the browser with zero Java dependencies.

### Truly Java-Free Alternatives

If you need a completely Java-free development experience, consider these alternatives:
- **[nbb](https://github.com/babashka/nbb)** - Node.js-based ClojureScript interpreter
- **[Lumo](https://github.com/anmonteiro/lumo)** - Self-hosted ClojureScript (archived but still usable)
- Manual ClojureScript compiler with JavaScript build tools

However, Shadow CLJS provides the best developer experience for ClojureScript with features like hot reload, source maps, and excellent error messages.

