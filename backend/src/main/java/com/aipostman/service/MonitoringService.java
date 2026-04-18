package com.aipostman.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitoringService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AtomicInteger fetchRequestCount = new AtomicInteger(0);
    private final AtomicInteger digestBuildCount = new AtomicInteger(0);
    private final AtomicInteger agentDiscoveryCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    
    @Autowired
    public MonitoringService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录抓取请求
     */
    public void recordFetchRequest() {
        fetchRequestCount.incrementAndGet();
    }

    /**
     * 记录日报构建
     */
    public void recordDigestBuild() {
        digestBuildCount.incrementAndGet();
    }

    /**
     * 记录agent发现
     */
    public void recordAgentDiscovery() {
        agentDiscoveryCount.incrementAndGet();
    }

    /**
     * 记录错误
     */
    public void recordError() {
        errorCount.incrementAndGet();
    }

    /**
     * 获取系统状态
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now());
        status.put("fetchRequestCount", fetchRequestCount.get());
        status.put("digestBuildCount", digestBuildCount.get());
        status.put("agentDiscoveryCount", agentDiscoveryCount.get());
        status.put("errorCount", errorCount.get());
        status.put("redisAvailable", isRedisAvailable());
        return status;
    }

    /**
     * 检查Redis是否可用
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 定期重置计数器
     */
    @Scheduled(cron = "0 0 0 * * *") // 每天凌晨重置
    public void resetCounters() {
        fetchRequestCount.set(0);
        digestBuildCount.set(0);
        agentDiscoveryCount.set(0);
        errorCount.set(0);
    }

    /**
     * 定期保存系统状态到Redis
     */
    @Scheduled(fixedRate = 60000) // 每分钟保存一次
    public void saveSystemStatus() {
        try {
            Map<String, Object> status = getSystemStatus();
            redisTemplate.opsForValue().set(
                    "system:status:latest",
                    status,
                    Duration.ofHours(24)
            );
        } catch (Exception e) {
            // 记录错误但不影响系统运行
            e.printStackTrace();
        }
    }
}
