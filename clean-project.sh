#!/bin/bash

# 项目清理脚本
# 删除不必要的文件，以减小项目大小

echo "=== AI Postman 项目清理脚本 ==="

echo "1. 删除构建产物"
# 删除后端构建产物
if [ -d "backend/target" ]; then
    echo "删除 backend/target..."
    rm -rf backend/target
    echo "✅ 已删除 backend/target"
fi

# 删除前端构建产物
if [ -d "frontend/dist" ]; then
    echo "删除 frontend/dist..."
    rm -rf frontend/dist
    echo "✅ 已删除 frontend/dist"
fi

echo "2. 删除依赖包"
# 删除 Python 虚拟环境
if [ -d ".venv" ]; then
    echo "删除 .venv..."
    rm -rf .venv
    echo "✅ 已删除 .venv"
fi

# 删除 Worker 服务的 Python 虚拟环境
if [ -d "worker/venv" ]; then
    echo "删除 worker/venv..."
    rm -rf worker/venv
    echo "✅ 已删除 worker/venv"
fi

# 删除前端 Node.js 依赖包
if [ -d "frontend/node_modules" ]; then
    echo "删除 frontend/node_modules..."
    rm -rf frontend/node_modules
    echo "✅ 已删除 frontend/node_modules"
fi

echo "3. 删除日志文件"
# 删除日志目录
if [ -d "logs" ]; then
    echo "删除 logs..."
    rm -rf logs
    echo "✅ 已删除 logs"
fi

# 删除后端日志目录
if [ -d "backend/logs" ]; then
    echo "删除 backend/logs..."
    rm -rf backend/logs
    echo "✅ 已删除 backend/logs"
fi

# 删除服务日志文件
if [ -f "worker.log" ]; then
    echo "删除 worker.log..."
    rm -f worker.log
    echo "✅ 已删除 worker.log"
fi

if [ -f "backend.log" ]; then
    echo "删除 backend.log..."
    rm -f backend.log
    echo "✅ 已删除 backend.log"
fi

if [ -f "frontend.log" ]; then
    echo "删除 frontend.log..."
    rm -f frontend.log
    echo "✅ 已删除 frontend.log"
fi

if [ -f "worker/worker.log" ]; then
    echo "删除 worker/worker.log..."
    rm -f worker/worker.log
    echo "✅ 已删除 worker/worker.log"
fi

echo "4. 删除备份文件"
# 删除备份文件
if [ -f "worker-backup-20260415153018.tar.gz" ]; then
    echo "删除 worker-backup-20260415153018.tar.gz..."
    rm -f worker-backup-20260415153018.tar.gz
    echo "✅ 已删除 worker-backup-20260415153018.tar.gz"
fi

echo "5. 检查项目大小"
echo "=== 项目清理后大小 ==="
du -h -d 2

echo "=== 项目清理完成！==="
echo "项目大小已减小，现在可以上传到服务器了。"
echo "在服务器上运行 ./install-deps.sh 安装所有必要的依赖。"
