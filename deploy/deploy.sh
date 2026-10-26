#!/usr/bin/env bash
# Build on the server and restart: run from the repository checkout on EC2.
#   ./deploy/deploy.sh            (uses the current branch)
set -euo pipefail
cd "$(dirname "$0")/.."
git pull --ff-only
mvn -B -DskipTests package
sudo install -o uteexpress -g uteexpress -m 0644 target/project-web-1.0.war /opt/uteexpress/app.war
sudo systemctl restart uteexpress
sleep 20
curl -fsS -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:8080/login
