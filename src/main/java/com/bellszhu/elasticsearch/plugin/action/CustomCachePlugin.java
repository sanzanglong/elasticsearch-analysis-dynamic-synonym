package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
//import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.*;
//import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
//import org.elasticsearch.tracing.Tracer;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xcontent.NamedXContentRegistry;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class CustomCachePlugin extends Plugin implements ActionPlugin {

    // 每个节点本地缓存（线程安全）
    private static final ConcurrentHashMap<String, String> NODE_LOCAL_CACHE = new ConcurrentHashMap<>();

    // 供 REST 接口读取
    public static String getCachedValue(String key) {
        return NODE_LOCAL_CACHE.get(key);
    }

    public static String setCachedValue(String key, String value) {
        return NODE_LOCAL_CACHE.put(key, value);
    }

    @Override
    public List<Setting<?>> getSettings() {
        // 可选：如果你想让用户通过 elasticsearch.yml 配置
        return Collections.emptyList();
    }

    NodeCacheService nodeCacheService = new NodeCacheService(NODE_LOCAL_CACHE);

//    @Override
//    public Collection<Object> createComponents(Client client,
//                                               ClusterService clusterService,
//                                               ThreadPool threadPool,
//                                               ResourceWatcherService resourceWatcherService,
//                                               ScriptService scriptService,
//                                               NamedXContentRegistry xContentRegistry,
//                                               Environment environment,
//                                               NodeEnvironment nodeEnvironment,
//                                               NamedWriteableRegistry namedWriteableRegistry,
//                                               IndexNameExpressionResolver indexNameExpressionResolver,
//                                               Supplier<RepositoriesService> repositoriesServiceSupplier,
//                                               Tracer tracer,
//                                               AllocationService allocationService) {
//        // ========== 这里就是在每个节点启动时执行的代码 ==========
//        String nodeId = nodeEnvironment.nodeId();
//        String nodeName = Settings.builder().put(environment.settings()).build().get("node.name", "unknown");
//        String uniqueValue = UUID.randomUUID().toString();
//
//        String cacheKey = "my_custom_data";
//        String cacheValue = String.format("nodeId=%s,nodeName=%s,uuid=%s,startedAt=%s",
//                nodeId, nodeName, uniqueValue, System.currentTimeMillis());
//
//        NODE_LOCAL_CACHE.put(cacheKey, "2");
//
//        System.out.println(">>> CustomCachePlugin initialized on node " + nodeId +
//                ", cached value: " + cacheValue);
//
//        // 如果你想注入成 ES 的 Component（可被注入），可以返回一个服务对象
//        return Collections.singletonList(nodeCacheService);
////        return Collections.emptyList();
//    }



    @Override
    public Collection<Object> createComponents(Client client,
                                               ClusterService clusterService,
                                               ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService,
                                               ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry,
                                               Environment environment,
                                               NodeEnvironment nodeEnvironment,
                                               NamedWriteableRegistry namedWriteableRegistry,
                                               IndexNameExpressionResolver indexNameExpressionResolver,
                                               Supplier<RepositoriesService> repositoriesServiceSupplier) {
        // ========== 这里就是在每个节点启动时执行的代码 ==========
        String nodeId = nodeEnvironment.nodeId();
        String nodeName = Settings.builder().put(environment.settings()).build().get("node.name", "unknown");
        String uniqueValue = UUID.randomUUID().toString();

        String cacheKey = "my_custom_data";
        String cacheValue = String.format("nodeId=%s,nodeName=%s,uuid=%s,startedAt=%s",
                nodeId, nodeName, uniqueValue, System.currentTimeMillis());

        NODE_LOCAL_CACHE.put(cacheKey, "2");

        System.out.println(">>> CustomCachePlugin initialized on node " + nodeId +
                ", cached value: " + cacheValue);

        // 如果你想注入成 ES 的 Component（可被注入），可以返回一个服务对象
        return Collections.singletonList(nodeCacheService);
    //        return Collections.emptyList();
    }

    // ---------- 可选：提供一个 REST 接口来查看缓存内容 ----------


    @Override
    public List<RestHandler> getRestHandlers(final Settings settings,
                                             final RestController restController,
                                             final ClusterSettings clusterSettings,
                                             final IndexScopedSettings indexScopedSettings,
                                             final SettingsFilter settingsFilter,
                                             final IndexNameExpressionResolver indexNameExpressionResolver,
                                             final Supplier<DiscoveryNodes> nodesInCluster) {

        return singletonList(new CustomCacheRestHandler(nodeCacheService));
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
                new ActionHandler<>(BroadcastCacheAction.INSTANCE, TransportBroadcastCacheAction.class)
        );
    }

    // 简单的服务类（如果其他插件需要注入）
    public static class NodeCacheService {
        private final ConcurrentHashMap<String, String> cache;
        public NodeCacheService(ConcurrentHashMap<String, String> cache) {
            this.cache = cache;
        }
        public String get(String key) { return cache.get(key); }
        public void set(String key, String value) { cache.put(key, value); }
    }
}