#!/usr/bin/env bash
set -euo pipefail
deploy_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=deploy/install-release.sh
source "$deploy_dir/install-release.sh"
repo_root=$(cd -- "$deploy_dir/.." && pwd -P)
mkdir -p "$repo_root/.local"
test_parent=$(cd -- "$repo_root/.local" && pwd -P)
[[ "$test_parent" == "$repo_root/.local" ]]
test_root=$(mktemp -d "$test_parent/deploy-test.XXXXXXXX")
cleanup_test() {
    [[ "$(dirname -- "$test_root")" == "$test_parent" && "$(basename -- "$test_root")" == deploy-test.* ]] || return 1
    rm -rf -- "$test_root"
}
trap cleanup_test EXIT
mkdir -p "$test_root/app/releases"
printf 'old-build' > "$test_root/app/app.war"
printf 'new-build' > "$test_root/incoming.war"
checksum=$(sha256sum "$test_root/incoming.war" | cut -d ' ' -f 1)
commit=1234567890123456789012345678901234567890
systemctl() { return 0; }
sleep() { :; }
healthy() { return 0; }
activate_release "$test_root/incoming.war" "$checksum" "$commit" "$test_root/app"
[[ $(cat "$test_root/app/app.war") == new-build ]]
[[ $(cat "$test_root/app/releases/previous.war") == old-build ]]
[[ $(cat "$test_root/app/releases/current-commit") == "$commit" ]]
printf 'broken-build' > "$test_root/incoming.war"
checksum=$(sha256sum "$test_root/incoming.war" | cut -d ' ' -f 1)
healthy() { [[ $(cat "$test_root/app/app.war") == new-build ]]; }
if activate_release "$test_root/incoming.war" "$checksum" "$commit" "$test_root/app"; then exit 1; fi
[[ $(cat "$test_root/app/app.war") == new-build ]]
[[ $(cat "$test_root/app/releases/current-commit") == "$commit" ]]
healthy() { return 1; }
if activate_release "$test_root/incoming.war" "$checksum" "$commit" "$test_root/app"; then exit 1; fi
[[ $(cat "$test_root/app/app.war") == new-build ]]
if activate_release "$test_root/incoming.war" "$(printf '%064d' 0)" "$commit" "$test_root/app"; then exit 1; fi
[[ $(cat "$test_root/app/app.war") == new-build ]]
MSYS=winsymlinks:nativestrict ln -s "$test_root/incoming.war" "$test_root/link.war"
[[ -L "$test_root/link.war" ]]
if activate_release "$test_root/link.war" "$checksum" "$commit" "$test_root/app"; then exit 1; fi
[[ $(cat "$test_root/app/app.war") == new-build ]]
if activate_release "$test_root/missing.war" "$checksum" "$commit" "$test_root/app"; then exit 1; fi
[[ $(cat "$test_root/app/app.war") == new-build ]]
echo 'Deployment success, rollback, checksum and symlink checks passed.'
