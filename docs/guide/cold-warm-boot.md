# Cold vs. warm boot: Clojurific against shadow-cljs

`bb bench` changes the deployed function's memory size, which forces a fresh
execution environment on the next invoke. It then measures one cold sample
and five warm ones back to back, and prints a table built from CloudWatch's
own `REPORT` line. `script/bench_run.clj` is the same orchestration
lambda-mvp-cljs uses, and `script/bench.clj` is the parser every
`lambda-mvp-*` sibling shares.

## Same day, same account, compiler as the only difference

Both projects were deployed to one account in `ap-southeast-2` on 2026-11-14,
arm64, managed `nodejs24.x`, and benched at the same time. Each ran
`bb bench` three times, and since a single Cold Init sample is too noisy to
call on its own, the table reports the median of those three runs with the
Cold Init range beside it.

| Metric | shadow-cljs, 2048 MB | Clojurific, 2048 MB | shadow-cljs, 3008 MB | Clojurific, 3008 MB |
|---|---|---|---|---|
| Cold Init Duration (median of 3) | 134.7 ms | 184.4 ms | 135.3 ms | 149.5 ms |
| Cold Init Duration (range of 3) | 105-135 ms | 145-186 ms | 135-135 ms | 147-192 ms |
| Cold Duration (median of 3) | 4.9 ms | 3.4 ms | 5.0 ms | 3.8 ms |
| Warm Duration (median of the 3 medians) | 1.8 ms | 1.8 ms | 1.9 ms | 1.6 ms |
| Max Memory Used (median of 3) | 77 MB | 81 MB | 77 MB | 82 MB |
| Deployed zip | 23,455 bytes | 77,485 bytes | same | same |

Each deploy's very first invoke, before any bench run, was cold too:
shadow-cljs reported 133.5 ms Init and 5.36 ms Duration, Clojurific 151.5 ms
Init and 2.98 ms Duration, both at 2048 MB.

### What the numbers say

**Cold Init is slower with Clojurific, by roughly 15 to 50 ms.** Every one of
its six cold samples landed between 145 and 192 ms, and every shadow-cljs
sample between 105 and 135 ms, so the two ranges don't overlap at all. Init
covers loading the handler module, and Clojurific's bundle is 3.3 times the
size of the `:advanced` one (347 KB against 105 KB), so Node has more
JavaScript to parse and evaluate before the first event. That's the most
likely cause, but nobody isolated it here, for example by padding the
shadow-cljs bundle to the same size.

**Cold Duration is faster with Clojurific, by about a third.** The handler's
first call took 3.0 to 4.4 ms against shadow-cljs's 4.3 to 6.8 ms, again
without overlap. Clojurific's README credits its runtime gains to call-site
protocol dispatch and `cljs.core` fast paths, and `js->clj`/`clj->js` on the
first event go through exactly that code. That fits, but a 1.5 ms gap on a
handler this small is a direction, not a measurement of the runtime work.

**Warm Duration is a tie.** Both sit between 1.6 and 2.0 ms, which is what a
handler doing one `swap!` and two conversions costs once V8 has warmed up.

**Memory is a few megabytes higher**, 81-82 MB against 77 MB, which lines up
with the larger module staying resident.

Added up, a cold invocation costs about the same either way. Clojurific's
extra Init time outweighs the milliseconds it saves inside the handler, and
`Billed Duration` on a managed runtime doesn't include Init anyway. The
benefits Clojurific advertises (faster builds, hot reload, a REPL, ES module
interop) are development-time ones that a one-shot Lambda build doesn't
exercise.

### How this compares with earlier rounds

lambda-mvp-cljs's own guide recorded 104.6 and 110.9 ms of Cold Init at
2048 MB in September 2026. Today's shadow-cljs median of 134.7 ms is higher,
with the same code and shadow-cljs version, so the account and region have
drifted by about 20-30 ms since then. Read the Clojurific numbers against
today's shadow-cljs column, not against the September ones.

## Reproducing a run

```sh
AWS_PROFILE=... AWS_REGION=... bb build
AWS_PROFILE=... AWS_REGION=... bb deploy
AWS_PROFILE=... AWS_REGION=... bb bench
AWS_PROFILE=... AWS_REGION=... bb teardown   # when you're done
```

`BENCH_MEMORY_TIERS` (default `2048,3008`) and `BENCH_WARM_SAMPLES` (default
`5`) are overridable, the same convention every sibling uses. Numbers from
your own account and region will differ, so treat the table above as
illustrative rather than a guarantee.
