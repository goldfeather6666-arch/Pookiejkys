#!/usr/bin/env bash
# ==============================================================================
# PIXEL LAB — Automated GitHub Repository Installer & Push Script
# Target Repository: https://github.com/goldfeather6666-arch/Pookiejkys.git
# ==============================================================================
set -euo pipefail

REPO_URL="https://github.com/goldfeather6666-arch/Pookiejkys.git"
ZIP_FILE="PIXEL_LAB_Pookiejkys_Complete_Repository.zip"

if [ ! -f "$ZIP_FILE" ]; then
  echo "Error: Place $ZIP_FILE in the same directory as this script before running."
  exit 1
fi

WORK_DIR="$(mktemp -d)"
echo "[1/4] Cloning $REPO_URL into $WORK_DIR..."
git clone "$REPO_URL" "$WORK_DIR/Pookiejkys"

echo "[2/4] Extracting PIXEL LAB 48-file Android codebase into repository..."
unzip -o "$ZIP_FILE" -d "$WORK_DIR"

cd "$WORK_DIR/Pookiejkys"
echo "[3/4] Staging all modules, docs, tests, and GitHub Actions workflows..."
git add -A

if git diff --cached --quiet; then
  echo "Repository is already up to date."
else
  git commit -m "Implement PIXEL LAB native Android suite (8 modules, Clean Architecture, CI/CD, docs & tests)"
  echo "[4/4] Pushing to origin/main..."
  git push origin main
  echo "Done! GitHub Actions is now building the PIXEL LAB Debug & Release APKs at:"
  echo "https://github.com/goldfeather6666-arch/Pookiejkys/actions"
fi