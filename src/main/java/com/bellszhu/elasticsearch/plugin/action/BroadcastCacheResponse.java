package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;

/**
 * 广播缓存响应 - 包含所有节点的响应结果
 */
public class BroadcastCacheResponse extends BaseNodesResponse<NodeCacheResponse> {

    public BroadcastCacheResponse(StreamInput in) throws IOException {
        super(in);
    }

    public BroadcastCacheResponse(ClusterName clusterName, List<NodeCacheResponse> nodes, List<FailedNodeException> failures) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<NodeCacheResponse> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(NodeCacheResponse::new);
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<NodeCacheResponse> nodes) throws IOException {
        out.writeList(nodes);
    }
}
