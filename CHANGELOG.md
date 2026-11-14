# Changelog

All notable changes to this project are documented here.

## Unreleased

### Added
- AWS Lambda demo handler in ClojureScript, compiled by Clojurific 0.12.15 to
  ES modules, bundled by Vite 8 into one `dist/index.mjs`, and deployed as a
  zip to AWS's managed `nodejs24.x` runtime.
- `handler.cljs`, the same handler as lambda-mvp-cljs apart from reading
  `context.awsRequestId` with plain dot access.
- `bb build`/`smoke`/`test`/`verify`/`demo`/`deploy`/`invoke`/`bench`/
  `teardown`/`clean`, with the deploy and bench scripts carried over from
  lambda-mvp-cljs.
- `docs/guide/clojurific-build.md` and `docs/guide/cold-warm-boot.md`, the
  latter with a same-day, three-run comparison against lambda-mvp-cljs.
