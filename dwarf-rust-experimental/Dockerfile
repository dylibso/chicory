FROM debian:bookworm-slim

# Use Docker's automatic build args for architecture and OS
ARG TARGETARCH
ARG TARGETOS

RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Rust
ENV RUSTUP_HOME=/usr/local/rustup
ENV CARGO_HOME=/usr/local/cargo
RUN curl -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH=/usr/local/cargo/bin:$PATH
RUN rustup target add wasm32-wasip1;
