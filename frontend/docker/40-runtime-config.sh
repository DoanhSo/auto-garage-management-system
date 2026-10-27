#!/bin/sh
set -eu
if ! printf '%s' "${KEYCLOAK_PUBLIC_URL:-}" | grep -Eq '^https?://[A-Za-z0-9._:-]+/realms/[A-Za-z0-9._-]+$'; then
    echo 'KEYCLOAK_PUBLIC_URL must be a full public realm URL without credentials or query parameters.' >&2
    exit 1
fi
envsubst '${KEYCLOAK_PUBLIC_URL}' < /opt/autogarage/runtime-config.js.template > /usr/share/nginx/html/runtime-config.js
