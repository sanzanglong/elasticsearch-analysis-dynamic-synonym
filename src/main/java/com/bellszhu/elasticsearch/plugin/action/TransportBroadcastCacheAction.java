package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.nodes.TransportNodesAction;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

/**
 * Transport Action - 处理广播缓存更新到所有节点
 */
public class TransportBroadcastCacheAction extends TransportNodesAction<
        BroadcastCacheRequest,
        BroadcastCacheResponse,
        NodeCacheRequest,
        NodeCacheResponse> {

    private final CustomCachePlugin.NodeCacheService cacheService;

    @Inject
    public TransportBroadcastCacheAction(
            ThreadPool threadPool,
            ClusterService clusterService,
            TransportService transportService,
            ActionFilters actionFilters,
            CustomCachePlugin.NodeCacheService cacheService) {
        super(
                BroadcastCacheAction.NAME,
                threadPool,
                clusterService,
                transportService,
                actionFilters,
                BroadcastCacheRequest::new,
                NodeCacheRequest::new,
                ThreadPool.Names.MANAGEMENT,
                NodeCacheResponse.class
        );
        this.cacheService = cacheService;
    }

    @Override
    protected BroadcastCacheResponse newResponse(
            BroadcastCacheRequest request,
            List<NodeCacheResponse> responses,
            List<FailedNodeException> failures) {
        return new BroadcastCacheResponse(clusterService.getClusterName(), responses, failures);
    }

    @Override
    protected NodeCacheRequest newNodeRequest(BroadcastCacheRequest request) {
        return new NodeCacheRequest(request);
    }

    @Override
    protected NodeCacheResponse newNodeResponse(StreamInput in, DiscoveryNode node) throws IOException {
        return new NodeCacheResponse(in);
    }

    @Override
    protected NodeCacheResponse nodeOperation(NodeCacheRequest request, org.elasticsearch.tasks.Task task) {
        try {
            // 在当前节点设置缓存
            cacheService.set(request.getKey(), request.getValue());
            
            String nodeName = clusterService.getNodeName();
            String message = String.format("Cache updated on node: %s, key=%s, value=%s", 
                    nodeName, request.getKey(), request.getValue());
            
            System.out.println(">>> " + message);
            
            return new NodeCacheResponse(clusterService.localNode(), true, message);
        } catch (Exception e) {
            String errorMessage = "Failed to update cache on node: " + clusterService.getNodeName() + 
                    ", error: " + e.getMessage();
            System.err.println(">>> " + errorMessage);
            e.printStackTrace();
            return new NodeCacheResponse(clusterService.localNode(), false, errorMessage);
        }
    }

}
