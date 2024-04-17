# jmh

This module is used to run JMH performance tests on Chicory and make it easy to compare results.

## Dependencies

There a few additional tools that are needed to run the benchmarks:

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
./scripts/show-results.sh
```
and open the generated link.

## From GH Actions

- Open the relevant Perf workflow.
...
