FROM rust:1.74

WORKDIR /usr/code

# Install wasm support for Rust
RUN rustup target add wasm32-unknown-unknown

# Install wasi-sdk
RUN wget https://github.com/WebAssembly/wasi-sdk/releases/download/wasi-sdk-20/wasi-sdk-20.0-linux.tar.gz && \
    mkdir -p /opt/wasi-sdk && \
    tar xvf wasi-sdk-20.0-linux.tar.gz && \
    cp -r wasi-sdk-20.0/* /opt/wasi-sdk

# Install wabt tools
RUN wget https://github.com/WebAssembly/wabt/releases/download/1.0.34/wabt-1.0.34-ubuntu.tar.gz && \
    mkdir -p /opt/wabt && \
    tar xvf wabt-1.0.34-ubuntu.tar.gz && \
    cp -r wabt-1.0.34/* /opt/wabt

ENV WASI_SDK_PATH=/opt/wasi-sdk

COPY . .
