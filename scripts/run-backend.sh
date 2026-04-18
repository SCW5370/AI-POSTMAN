#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/../backend"
if [ -f ../.env ]; then
  set -a
  source ../.env
  set +a
fi
# 清除所有代理环境变量（包括 SOCKS）
unset http_proxy https_proxy all_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY
unset socks_proxy SOCKS_PROXY socks5_proxy SOCKS5_PROXY
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS

NON_PROXY_HOSTS="localhost|127.0.0.1|127.*|::1|postgres|redis|worker|backend"

# 显式清空 JVM 的 SOCKS 代理（防止 JVM 从系统配置读到代理）
# -DsocksProxyHost= 设为空字符串，覆盖任何已有配置
export JAVA_TOOL_OPTIONS="\
  -Djava.net.useSystemProxies=false \
  -DproxySet=false \
  -DsocksProxyHost= \
  -DsocksProxyPort= \
  -Dhttp.proxyHost= \
  -Dhttp.proxyPort= \
  -Dhttps.proxyHost= \
  -Dhttps.proxyPort= \
  -Dhttp.nonProxyHosts=${NON_PROXY_HOSTS} \
  -Dhttps.nonProxyHosts=${NON_PROXY_HOSTS}"

export MAVEN_OPTS="\
  -Djava.net.useSystemProxies=false \
  -DproxySet=false \
  -DsocksProxyHost= \
  -DsocksProxyPort= \
  -Dhttp.nonProxyHosts=${NON_PROXY_HOSTS} \
  -Dhttps.nonProxyHosts=${NON_PROXY_HOSTS}"

export NO_PROXY="localhost,127.0.0.1,::1,postgres,redis,worker,backend"
export no_proxy="$NO_PROXY"

mvn spring-boot:run
