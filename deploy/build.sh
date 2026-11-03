#!/usr/bin/env bash
# 构建后端 jar 与前端静态资源，产物位于 MindMate_backend/**/target 与 MindMate_frontend/dist
set -euo pipefail

# 需要 JDK 17 构建后端，JDK 8/11 会报“无效的目标发行版: 17”

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> 构建后端"
cd "$ROOT/MindMate_backend"
mvn -DskipTests clean package

echo "==> 构建前端"
cd "$ROOT/MindMate_frontend"
npm ci
npm run build

echo "==> 构建完成"
ls -lh "$ROOT"/MindMate_backend/mindmate-auth/target/*.jar
du -sh "$ROOT/MindMate_frontend/dist"
