# lambda-mvp-cljrfc, user guide

`lambda-mvp-cljrfc` runs a ClojureScript handler on AWS Lambda's managed `nodejs24.x` runtime, compiled by [Clojurific](https://github.com/WhimsicalCode/clojurific) instead of shadow-cljs. Clojurific is a ClojureScript fork that writes one ES module per namespace and leaves bundling to Vite, so there's no Closure Compiler anywhere in the build.

It has a twin, [lambda-mvp-cljs](https://github.com/b12n-oss/lambda-mvp-cljs). The handler, the runtime, and the deploy and bench scripts are the same in both repos, and only the compiler differs. When the two benchmark differently, the compiler and the bundle it produces are why.

The `aws` commands use whatever your CLI already has configured (`AWS_PROFILE`/`AWS_REGION`, or `aws configure`). No profile, account or region is hardcoded, so the commands that produced the numbers in this guide will produce yours on your own account.

## How to read this guide

If you just want to see it run, start with [Getting started](getting-started.md). It takes you from a clean checkout through the local smoke test to a live deploy, and ends with the teardown.

[How Clojurific builds the handler](clojurific-build.md) follows a namespace all the way to the single `dist/index.mjs` that goes in the zip. It also covers why `(.-awsRequestId context)` works here when the shadow-cljs twin has to use `unchecked-get`, and what the larger bundle costs.

The numbers are in [Cold vs. warm boot](cold-warm-boot.md), measured against lambda-mvp-cljs on the same day, in the same account and region. Read it before quoting a figure from the README, because the ranges matter more than any single median.

## Guide map

| Page | What you'll learn |
|---|---|
| [Getting started](getting-started.md) | Requirements, the local smoke test, a first deploy, teardown |
| [How Clojurific builds the handler](clojurific-build.md) | Namespace to ES module to one bundled file, dot access on `context`, bundle size |
| [Cold vs. warm boot](cold-warm-boot.md) | Clojurific against shadow-cljs on Lambda, three runs per tier |

## Find your scenario

| Scenario | Pages to read |
|---|---|
| "I want to try it" | Getting started |
| "I'm deciding between Clojurific and shadow-cljs for a Lambda" | Cold vs. warm boot, then How Clojurific builds the handler |
| "I want to know why the bundle is three times bigger" | How Clojurific builds the handler |
| "I want my own numbers" | Getting started, then the reproduction steps at the end of Cold vs. warm boot |

## See also

- [Project README](https://github.com/b12n-oss/lambda-mvp-cljrfc/blob/main/README.md) has the quickstart and a short version of the benchmark table.
- [`CHANGELOG.md`](https://github.com/b12n-oss/lambda-mvp-cljrfc/blob/main/CHANGELOG.md) lists what changed and when.
- lambda-mvp-bb's [five-way comparison](https://github.com/b12n-oss/lambda-mvp-bb/blob/main/docs/guide/five-way-comparison.md) puts the Babashka, ClojureScript, Jolt and jank siblings side by side.
