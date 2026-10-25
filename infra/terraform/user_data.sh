#!/usr/bin/env bash
set -Eeuo pipefail

dnf install -y docker curl
systemctl enable --now docker
systemctl enable --now amazon-ssm-agent

install -d -m 700 /etc/boothfit /opt/boothfit
install -m 600 /dev/null /etc/boothfit/app.env
