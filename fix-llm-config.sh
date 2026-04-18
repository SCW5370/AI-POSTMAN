#!/bin/bash

# 修复 LLM 配置脚本

echo "=== 修复 LLM 配置 ==="

# 配置 LLM 环境变量
echo "配置 LLM 环境变量"
cat > /opt/ai-postman/worker/.env << EOF
# Worker LLM 配置
# 留空则自动继承根目录 .env 中的 LLM_BASE_URL / LLM_API_KEY / LLM_MODEL
# 如需单独覆盖 Worker 的 LLM 配置，在此填写：
# LLM_API_BASE=
# LLM_API_KEY=
# LLM_MODEL=
EOF

# 安装兼容的依赖
echo "安装兼容的依赖"
cd /opt/ai-postman/worker
if [ -d "venv" ]; then
    echo "清理旧的虚拟环境"
    rm -rf venv
fi

echo "创建新的虚拟环境"
python3 -m venv venv

echo "安装依赖"
source venv/bin/activate
pip install --upgrade pip
pip install wheel
pip install langchain-core==0.1.52
pip install langchain-community==0.0.38
pip install -r requirements.txt
deactivate

# 重启 Worker 服务
echo "重启 Worker 服务"
pkill -f "uvicorn app.main:app"
source venv/bin/activate
nohup uvicorn app.main:app --host 127.0.0.1 --port 8000 > worker.log 2>&1 &
deactivate

# 等待 Worker 启动
echo "等待 Worker 服务启动..."
sleep 5

# 检查 Worker 服务状态
echo "检查 Worker 服务状态"
if ps aux | grep -E "uvicorn app.main:app" | grep -v grep > /dev/null; then
    echo "Worker 服务启动成功"
else
    echo "Worker 服务启动失败！"
    echo "查看 Worker 日志:"
    if [ -f "worker.log" ]; then
        tail -n 20 worker.log
    else
        echo "Worker 日志文件不存在"
    fi
    exit 1
fi

echo "=== LLM 配置修复完成 ==="
