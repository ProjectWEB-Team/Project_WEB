#!/usr/bin/env bash
# Install once as root-owned /usr/local/sbin/uteexpress-install-release on the VPS.
set -euo pipefail

healthy() {
    local expected=$1 payload
    payload=$(curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8080/health/ready) || return 1
    if [[ -n "$expected" ]]; then
        jq -e --arg commit "$expected" '.status == "UP" and .commit == $commit' <<< "$payload" > /dev/null
    else
        jq -e '.status == "UP"' <<< "$payload" > /dev/null
    fi
}

wait_for_release() {
    local attempt
    for ((attempt=0; attempt<30; attempt++)); do
        if healthy "$1"; then return 0; fi
        sleep 4
    done
    return 1
}

activate_release() {
    local incoming=$1 checksum=$2 commit=$3 app_root=$4 staging previous previous_commit=''
    staging=$(mktemp -d "$app_root/releases/.stage-XXXXXXXX")
    previous="$app_root/releases/previous.war"
    # Copy symlinks as symlinks, then reject them inside a root-owned private staging directory.
    cp --no-dereference -- "$incoming" "$staging/app.war" || { rmdir -- "$staging"; return 1; }
    if [[ ! -f "$staging/app.war" || -L "$staging/app.war" ]]; then
        echo 'Artifact must be a regular file.' >&2
        rm -f -- "$staging/app.war"; rmdir -- "$staging"; return 1
    fi
    if [[ "$(sha256sum "$staging/app.war" | cut -d ' ' -f 1)" != "$checksum" ]]; then
        echo 'Artifact checksum mismatch.' >&2
        rm -f -- "$staging/app.war"; rmdir -- "$staging"; return 1
    fi
    if [[ -f "$app_root/app.war" ]]; then
        cp -- "$app_root/app.war" "$previous"
        if [[ -f "$app_root/releases/current-commit" ]]; then
            previous_commit=$(cat "$app_root/releases/current-commit")
        fi
    else
        rm -f -- "$previous"
    fi
    chmod 644 "$staging/app.war"
    mv -f -- "$staging/app.war" "$app_root/app.war"
    rmdir -- "$staging"
    if systemctl restart uteexpress && wait_for_release "$commit"; then
        printf '%s\n' "$commit" > "$app_root/releases/current-commit"
        echo "Deployed $commit"
        return 0
    fi
    echo 'New release failed readiness; restoring the previous artifact.' >&2
    if [[ -f "$previous" ]]; then
        cp -- "$previous" "$app_root/releases/rollback.war"
        mv -f -- "$app_root/releases/rollback.war" "$app_root/app.war"
        if systemctl restart uteexpress && wait_for_release "$previous_commit"; then
            echo 'Previous release restored and ready.' >&2
        else
            echo 'Previous artifact restored but readiness failed; inspect the service logs.' >&2
        fi
    else
        echo 'No previous artifact is available; inspect the service logs.' >&2
    fi
    # Database migrations are never rolled back automatically.
    return 1
}

main() {
    [[ $EUID -eq 0 ]] || { echo 'Run through the installed sudo rule.' >&2; return 1; }
    [[ $# -eq 2 && "$1" =~ ^[a-f0-9]{40}$ && "$2" =~ ^[a-f0-9]{64}$ ]] || return 2
    local app_root=/opt/uteexpress
    [[ -d "$app_root" && ! -L "$app_root" && "$(stat -c '%u' "$app_root")" == 0 ]] || {
        echo '/opt/uteexpress must be a root-owned directory.' >&2; return 1;
    }
    (( (8#$(stat -c '%a' "$app_root") & 022) == 0 )) || return 1
    [[ ! -L "$app_root/releases" ]] || return 1
    install -d -m 755 -o root -g root "$app_root/releases"
    exec 9> "$app_root/releases/deploy.lock"
    flock -w 180 9 || { echo 'Another deployment is still running.' >&2; return 1; }
    activate_release "/var/lib/uteexpress-deploy/incoming/$1/app.war" "$2" "$1" "$app_root"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then main "$@"; fi
