#!/usr/bin/env bash
# Run on a fresh Azure Ubuntu 22.04 VM after SSH login:
#   curl -fsSL ... | bash
# or: bash bootstrap-vm.sh
set -euo pipefail

echo "==> Installing Docker Engine + Compose plugin"
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "${USER}"

echo "==> Creating /opt/ingoboka"
sudo mkdir -p /opt/ingoboka/deploy
sudo chown -R "${USER}:${USER}" /opt/ingoboka

echo "==> Done."
echo "Log out and SSH back in so the 'docker' group applies, then:"
echo "  cd /opt/ingoboka/deploy"
echo "  # place Dockerfile + sources + .env, then:"
echo "  docker compose up -d --build"
echo ""
echo "Public URLs (replace YOUR_IP):"
echo "  API:     http://YOUR_IP:8085/api/v1"
echo "  Swagger: http://YOUR_IP:8085/swagger-ui.html"
echo "  Health:  http://YOUR_IP:8085/actuator/health"
echo "  USSD:    http://YOUR_IP:8085/api/v1/ussd/callback"
