package com.aipostman.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存数据
     */
    public <T> void set(String key, T value, Duration expiration) {
        redisTemplate.opsForValue().set(key, value, expiration);
    }

    /**
     * 获取缓存数据
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of((T) value) : Optional.empty();
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 生成源发现缓存键
     */
    public String generateSourceDiscoveryKey(String topic) {
        return "source:discovery:" + topic;
    }

    /**
     * 生成内容推荐缓存键
     */
    public String generateContentRecommendationKey(Long userId, String topic) {
        return "content:recommendation:" + userId + ":" + topic;
    }

    /**
     * 生成用户画像缓存键
     */
    public String generateUserProfileKey(Long userId) {
        return "user:profile:" + userId;
    }
}
