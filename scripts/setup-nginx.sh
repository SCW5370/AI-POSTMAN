#!/bin/bash

# 一键配置Nginx反向代理脚本

# 颜色输出
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
NC="\033[0m" # No Color

echo -e "${GREEN}=== 开始配置Nginx反向代理 ===${NC}"

# 检查是否以root权限运行
if [ "$(id -u)" != "0" ]; then
    echo -e "${RED}错误：请以root权限运行此脚本${NC}"
    exit 1
fi

# 获取服务器公网IP
PUBLIC_IP=$(curl -s ifconfig.me)
if [ -z "$PUBLIC_IP" ]; then
    echo -e "${YELLOW}警告：无法获取公网IP，请手动输入${NC}"
    read -p "请输入服务器公网IP: " PUBLIC_IP
fi

echo -e "${GREEN}检测到服务器公网IP: ${PUBLIC_IP}${NC}"

# 检查Nginx是否已安装
if ! command -v nginx &> /dev/null; then
    echo -e "${YELLOW}Nginx未安装，正在安装...${NC}"
    apt update && apt install -y nginx
    if [ $? -ne 0 ]; then
        echo -e "${RED}Nginx安装失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}Nginx安装成功${NC}"
else
    echo -e "${GREEN}Nginx已安装${NC}"
fi

# 创建Nginx配置文件
NGINX_CONFIG="/etc/nginx/sites-available/ai-postman"
cat > "$NGINX_CONFIG" << EOF
server {
    listen 80;
    server_name $PUBLIC_IP;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
EOF

if [ $? -ne 0 ]; then
    echo -e "${RED}创建Nginx配置文件失败${NC}"
    exit 1
fi

echo -e "${GREEN}创建Nginx配置文件成功${NC}"

# 启用配置
if [ -f "/etc/nginx/sites-enabled/ai-postman" ]; then
    rm -f "/etc/nginx/sites-enabled/ai-postman"
fi

ln -s "$NGINX_CONFIG" "/etc/nginx/sites-enabled/"
if [ $? -ne 0 ]; then
    echo -e "${RED}启用Nginx配置失败${NC}"
    exit 1
fi

echo -e "${GREEN}启用Nginx配置成功${NC}"

# 测试Nginx配置
nginx -t
if [ $? -ne 0 ]; then
    echo -e "${RED}Nginx配置测试失败${NC}"
    exit 1
fi

echo -e "${GREEN}Nginx配置测试成功${NC}"

# 重启Nginx服务
systemctl restart nginx
if [ $? -ne 0 ]; then
    echo -e "${RED}重启Nginx服务失败${NC}"
    exit 1
fi

echo -e "${GREEN}重启Nginx服务成功${NC}"

# 检查Nginx服务状态
systemctl status nginx --no-pager

# 显示访问地址
echo -e "${GREEN}=== Nginx配置完成 ===${NC}"
echo -e "${GREEN}前端访问地址: http://$PUBLIC_IP${NC}"
echo -e "${GREEN}后端API地址: http://$PUBLIC_IP/api${NC}"
echo -e "${GREEN}请确保前端和后端服务已启动${NC}"
