// Clojurific compiles the handler namespace to ES modules, one per
// namespace, under target/cljs. The :js-entries entry `lambda` is a
// generated module exporting the handler var under the JavaScript name
// `handler`. Vite then bundles that entry for Node (an SSR build) into
// one self-contained file, dist/index.mjs, which the managed nodejs24.x
// runtime loads as an ES module (handler: index.handler).
import { defineConfig } from 'vite';
import cljf from 'clojurific/vite';

const entry = 'target/cljs/cljs-esm-entries/lambda.js';

export default defineConfig({
  plugins: [
    cljf({
      outputDir: 'target/cljs',
      compilerOptions: {
        jsEntries: { lambda: { exports: { handler: "'net.b12n.lambda-mvp.handler/handler" } } },
        optimizeConstants: true,
      },
      // a Lambda function has no pages, so no manifest of their scripts
      manifest: false,
    }),
  ],
  build: {
    ssr: entry,
    target: 'node24',
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: { output: { entryFileNames: 'index.mjs', format: 'es' } },
  },
  // bundle everything (cljs.core and any npm deps) into index.mjs, so the
  // zip needs no node_modules
  ssr: { noExternal: true, target: 'node' },
});
