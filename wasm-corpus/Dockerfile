FROM ubuntu:jammy

# Set the environment variable to avoid prompts
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y curl build-essential gcc make pkg-config libssl-dev dotnet-sdk-8.0 unzip git gnupg2 libcurl4-openssl-dev libedit2 libpython3-dev libxml2-dev libz3-dev python3-lldb-13 tzdata zlib1g-dev openjdk-21-jdk nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"
RUN rustc --version && cargo --version
RUN rustup target add wasm32-unknown-unknown
RUN rustup target add wasm32-wasip1

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


# Install Go
ENV GO_VERSION=1.21.7
RUN curl -L -O https://golang.org/dl/go${GO_VERSION}.linux-amd64.tar.gz \
    && tar -C /usr/local -xzf go${GO_VERSION}.linux-amd64.tar.gz \
    && rm go${GO_VERSION}.linux-amd64.tar.gz

# Install dotnet
RUN dotnet workload install wasi-experimental

# Set the Go environment variables
ENV PATH=$PATH:/usr/local/go/bin
ENV GOPATH=/go
ENV PATH=$PATH:$GOPATH/bin

# Install tinygo
RUN curl -L -O https://github.com/tinygo-org/tinygo/releases/download/v0.30.0/tinygo_0.30.0_amd64.deb && \
    dpkg -i tinygo_0.30.0_amd64.deb

# Install Swift
RUN curl -O https://download.swift.org/swiftly/linux/swiftly-$(uname -m).tar.gz && \
    tar zxf swiftly-$(uname -m).tar.gz && \
    ./swiftly init --assume-yes && \
    ~/.local/share/swiftly/bin/swiftly install 6.2-SNAPSHOT && \
    ~/.local/share/swiftly/bin/swift sdk install "https://download.swift.org/swift-6.2-branch/wasm-sdk/swift-6.2-DEVELOPMENT-SNAPSHOT-2025-07-09-a/swift-6.2-DEVELOPMENT-SNAPSHOT-2025-07-09-a_wasm.artifactbundle.tar.gz" --checksum "a26426852195f876f94e56d7819d94090de12fcfe158c8d53d5453f9467f7190"

ENV PATH="/opt/wabt/bin:~/.local/share/swiftly/bin:${PATH}"

# Install wasm-tools
RUN cargo install wasm-tools

WORKDIR /usr/code

ENTRYPOINT ["./compile.sh"]