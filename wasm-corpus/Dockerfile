FROM ubuntu:jammy

# Set the environment variable to avoid prompts
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y curl build-essential gcc make pkg-config libssl-dev && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"
RUN rustc --version && cargo --version
RUN rustup target add wasm32-unknown-unknown
RUN rustup target add wasm32-wasi

WORKDIR /tmp

# Install wasi-sdk
RUN curl -L -O https://github.com/WebAssembly/wasi-sdk/releases/download/wasi-sdk-20/wasi-sdk-20.0-linux.tar.gz && \
    mkdir -p /opt/wasi-sdk && \
    tar xvf wasi-sdk-20.0-linux.tar.gz && \
    cp -r wasi-sdk-20.0/* /opt/wasi-sdk

ENV WASI_SDK_PATH=/opt/wasi-sdk

# Install wabt tools
RUN curl -L -O https://github.com/WebAssembly/wabt/releases/download/1.0.34/wabt-1.0.34-ubuntu.tar.gz && \
    mkdir -p /opt/wabt && \
    tar xvf wabt-1.0.34-ubuntu.tar.gz && \
    cp -r wabt-1.0.34/* /opt/wabt

# Install Javy
RUN curl -L -O https://github.com/bytecodealliance/javy/releases/download/v1.4.0/javy-x86_64-linux-v1.4.0.gz && \
    mkdir -p /opt/javy && \
    gunzip javy-x86_64-linux-v1.4.0.gz && \
    mv javy-x86_64-linux-v1.4.0 /usr/bin/javy && \
    chmod a+x /usr/bin/javy

ENV PATH="/opt/wabt/bin:${PATH}"

WORKDIR /usr/code

ENTRYPOINT ["./compile.sh"]