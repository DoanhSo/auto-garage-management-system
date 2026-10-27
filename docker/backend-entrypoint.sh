#!/bin/sh
set -eu
if [ -f /run/autogarage/identity-id ]; then
    identity=$(cat /run/autogarage/identity-id)
    if ! printf '%s' "$identity" | grep -Eq '^[A-Za-z0-9-]{1,36}$'; then
        echo 'Invalid initialized Keycloak identity' >&2
        exit 1
    fi
    export AUTH_BOOTSTRAP_IDENTITY_ID="$identity"
fi
exec java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar
