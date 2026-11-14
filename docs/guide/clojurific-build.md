# How Clojurific builds the handler

This project and [lambda-mvp-cljs](https://github.com/b12n-oss/lambda-mvp-cljs)
deploy the same handler to the same managed `nodejs24.x` runtime. The one
thing that differs is the compiler: lambda-mvp-cljs uses shadow-cljs and the
Closure Compiler's `:advanced` mode, while this project uses
[Clojurific](https://github.com/WhimsicalCode/clojurific), a ClojureScript
fork that emits ES modules and lets Vite bundle them. So any gap between the
two projects' numbers comes from the compiler and its bundle.

## From namespace to `dist/index.mjs`

`bb build` runs `npx vite build`. The Clojurific Vite plugin
(`clojurific/vite`) starts the compiler first. It downloads
`com.whimsical/clojurific` from Clojars through its own `cljf` launcher, so
neither the Clojure CLI nor shadow-cljs is involved, and writes one ES module
per namespace into `target/cljs/`:

```
target/cljs/cljs/core.js
target/cljs/net/b12n/lambda_mvp/handler.js
target/cljs/cljs-esm-entries/lambda.js     <- the :js-entries module
```

The last file comes from `vite.config.mjs`:

```js
jsEntries: { lambda: { exports: { handler: "'net.b12n.lambda-mvp.handler/handler" } } }
```

That generates a module whose only job is
`export { handler }`, the Clojurific equivalent of shadow-cljs's
`:node-library` with `:exports {:handler ...}`. Vite then runs an SSR build
with that module as its input, and `ssr.noExternal: true` makes it inline
everything into a single file, `dist/index.mjs`. The zip holds only that file.
`bb deploy` points `--handler index.handler` at it, and because the extension
is `.mjs`, the managed runtime loads it as an ES module and calls its
`handler` export.

Before the bundle is written, the plugin removes protocol implementations that
nothing in the bundle calls, then minifies with oxc and shortens the
compiler's own `cljs$...` property names. For this handler the build log
reports 1,667 removed statements and 488 renamed properties.

## Dot access on the context object

lambda-mvp-cljs has to read the request id with
`(unchecked-get context "awsRequestId")`. Under `:advanced`, Closure renames
property names it can't prove belong to an external object, so
`(.-awsRequestId context)` compiles to a lookup of some shortened name and
silently returns `undefined`.

Clojurific has no Closure Compiler and no externs. The only properties its
minifier renames are the ones the compiler itself generates (`cljs$core$...`),
so a host object's `awsRequestId` keeps its name. This handler uses the plain
`(.-awsRequestId context)`, and `bb smoke` checks the request id round-trips
through the minified bundle.

## The bundle is bigger

| | lambda-mvp-cljs (shadow-cljs `:advanced`) | this project (Clojurific + Vite) |
|---|---|---|
| bundled JS | 104,754 bytes (`dist/index.js`) | 346,545 bytes (`dist/index.mjs`) |
| deployed zip | 23,455 bytes | 77,485 bytes |

Both were built from the same handler on 2026-11-14. Closure's whole-program
dead-code removal is still more aggressive than Vite's tree shaking plus
Clojurific's protocol pruning. Clojurific's own README puts its hello world at
177 KB against Closure's 110 KB. This handler's `clj->js` and `js->clj` calls
pull in a lot of `cljs.core`, and the gap widens further from there. Whether
an extra 230 KB of JavaScript costs anything at cold start is one of the
things [the benchmark](cold-warm-boot.md) measures.

## What this project doesn't use

Clojurific's headline features are a dev server with hot reload, an nREPL into
the running process, and an MCP server for coding agents. A Lambda handler
built once and zipped uses none of them: the build here is `vite build` and
nothing else. To develop a handler interactively you'd add `repl: { nreplPort: 0 }`
to the plugin's `compilerOptions` and run the handler under `vite`'s SSR
environment, the way the Clojurific Hono template runs its server. This repo
leaves that out so its build stays a plain release build, like the shadow-cljs
twin's.
