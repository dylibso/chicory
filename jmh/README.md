# jmh

This module is used to run JMH performance tests on Chicory and make it easy to compare results.

## Dependencies

There a few additional tools that are needed to see the benchmark results:

- `wget`
- `http-server` install with: `npm install http-server -g`

## Locally

### Prepare the baseline

The baseline is produced out of main, prepare it by running:

```bash
./scripts/build-jmh-main.sh
./scripts/run-jmh-main.sh
```

### Run the JMH benchmarks

From the current local folder you can produce the JMH results running:

```bash
./scripts/build-jmh.sh
./scripts/run-jmh.sh
```

### Show the results

```bash
./scripts/show-results.sh local
```
and open the generated link.

## From GH Actions

If you run the JMH tests from a GH Action:

- Open the relevant GH Action Summary view of the Perf run
- Follow the steps described

```bash
./scripts/show-results.sh ci <link-to-baseline-results> <link-to-current-results>
```

Or using a container image:

```bash
docker run --rm -it -p 3000:3000 docker.io/andreatp/chicory-show-jmh ci <link-to-baseline-results> <link-to-current-results>
```
