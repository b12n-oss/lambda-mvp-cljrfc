# Getting started

## What you need

Node.js 20 or later and `npm`, plus Java 17 or later. The compiler runs on the JVM, but you don't install it yourself: the Vite plugin starts Clojurific's `cljf` launcher, which downloads `com.whimsical/clojurific` from Clojars the first time you build. You don't need the Clojure CLI either. On a machine with no Java, `npx cljf setup-java` installs a Temurin JRE.

You'll also want `zip`, [babashka](https://babashka.org) for the `bb` tasks, and AWS CLI v2 once you deploy.

## Build and test locally

```sh
npm ci
bb verify
```

`bb verify` runs the bench parser's unit tests, builds `dist/lambda.zip`, then loads the bundled handler under Node and calls it twice. The smoke test checks that the warm-invocation counter goes from 1 to 2, that the request id comes back from the context object, and that the event is echoed. None of it touches AWS.

The first build is slower than later ones, since it downloads the compiler and fills Clojurific's per-user caches. On the machine these docs were written on it took about 18 seconds, and each rebuild after that took a few seconds.

## Deploy and invoke

Point the AWS CLI at the account and region you want, then:

```sh
export AWS_PROFILE=... AWS_REGION=...
bb deploy
bb invoke
```

`bb deploy` creates an IAM role called `lambda-mvp-cljrfc-role` and a function called `lambda-mvp-cljrfc`, or updates the function's code if it already exists. Set `LAMBDA_MVP_FUNCTION_NAME` to use another name, and make sure that name isn't something you care about, because `bb teardown` deletes it. The function runs on arm64 unless you set `LAMBDA_ARCH=x86_64`.

`bb invoke` prints the response body and the tail of the function's log, including CloudWatch's `REPORT` line. On the first invoke that line has an `Init Duration`, because the function started cold.

`bb demo` does the build, deploy and invoke in one go, and checks your tools and AWS credentials before it creates anything.

## Benchmark, then tear down

```sh
bb bench
bb teardown
```

[Cold vs. warm boot](cold-warm-boot.md) explains what `bb bench` measures and how to read its table. It leaves the function at the last memory tier it tried, 3008 MB by default, so don't skip the teardown. `bb teardown` deletes the function and the role. To confirm nothing is left:

```sh
aws lambda list-functions --query 'Functions[?starts_with(FunctionName, `lambda-mvp`)].FunctionName'
aws iam list-roles --query 'Roles[?starts_with(RoleName, `lambda-mvp`)].RoleName'
```
