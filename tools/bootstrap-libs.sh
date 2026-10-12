#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
while read -r hash path url; do
    case "$hash" in ''|'#'*) continue ;; esac
    mkdir -p "$(dirname "$path")"
    if [ ! -f "$path" ]; then
        if command -v curl >/dev/null 2>&1; then curl --fail --location --max-time 60 "$url" -o "$path";
        else wget -T 60 -O "$path" "$url"; fi
    fi
    printf '%s  %s\n' "$hash" "$path" | sha256sum -c -
done < tools/dependencies.lock
