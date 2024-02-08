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

# Install Javy
RUN wget https://github.com/bytecodealliance/javy/releases/download/v1.4.0/javy-x86_64-linux-v1.4.0.gz && \
    mkdir -p /opt/javy && \
    gunzip javy-x86_64-linux-v1.4.0.gz && \
    mv javy-x86_64-linux-v1.4.0 /opt/javy/javy && \
    chmod a+x /opt/javy/javy

ENV WASI_SDK_PATH=/opt/wasi-sdk

COPY . .
