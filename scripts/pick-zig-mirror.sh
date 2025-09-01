#!/usr/bin/env bash
set -euo pipefail

mirrors=$(curl -s https://ziglang.org/download/community-mirrors.txt)

for url in $(echo "$mirrors" | shuf); do
  curl_opts=(--head --fail --silent --show-error)
  if curl "${curl_opts[@]}" "$url/" > /dev/null 2>&1; then
    echo "$url"
    exit 0
  fi
done

echo "https://ziglang.org/download"
