package com.bellszhu.elasticsearch.plugin.ingest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GlobalCache {
    // 单例
    private static final GlobalCache INSTANCE = new GlobalCache();

    // 你的缓存结构（随便改）
    private final ConcurrentHashMap<String, String> kv = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    private GlobalCache() {}

    public static GlobalCache getInstance() {
        return INSTANCE;
    }

    // 写缓存（Ingest 插件调用）
    public void put(String key, String value) {
        kv.put(key, value);
        counter.incrementAndGet();
    }

    // 读缓存（Search 插件调用）
    public String get(String key) {
        return kv.get(key);
    }

    public long size() {
        return kv.size();
    }

    public long getCounter() {
        return counter.get();
    }

    public void clear() {
        kv.clear();
        counter.set(0);
    }
}