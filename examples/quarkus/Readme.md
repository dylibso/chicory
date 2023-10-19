
# Quarkus integration example

Run with docker:

```
docker run --rm -it -p 8080:8080 docker.io/andreatp/chicory-demo
```

Compile the rust demo program:

```
(cd demo-rust && ./compile.sh)
```

Now send the program to the Quarkus application:

```
curl -v 'http://localhost:8080/wasm' -H 'Content-Type: application/octet-stream' --data-binary @demo-rust/main.wasm
```

and now the server can process with the WASM code your requests:

```
curl -v 'http://localhost:8080/compute' -H 'Content-Type: application/json' --data-raw '{"content": "foo"}'
```
