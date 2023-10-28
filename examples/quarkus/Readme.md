
# Quarkus integration example

## Build

To build the container image run:

```
./build-docker.sh
```

## Run

Run with docker:

```
docker run --rm -it -p 8080:8080 docker.io/andreatp/chicory-demo
```

Compile the rust demo program:

```
(cd demo-rust && ./compile.sh)
```

You can now use the minimal UI available at: http://localhost:8080

Or use curl to send the program to the Quarkus application:

```
curl -v 'http://localhost:8080/wasm' -H 'Content-Type: application/octet-stream' --data-binary @demo-rust/main.wasm
```

and now the server can process with the WASM code your requests:

```
curl -v 'http://localhost:8080/compute' -H 'Content-Type: application/json' --data-raw '{"content": "foo"}'
```
