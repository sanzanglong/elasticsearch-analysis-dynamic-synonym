package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * 单个节点的缓存响应
 */
public class NodeCacheResponse extends BaseNodeResponse {

    private boolean success;
    private String message;

    public NodeCacheResponse(StreamInput in) throws IOException {
        super(in);
        this.success = in.readBoolean();
        this.message = in.readString();
    }

    public NodeCacheResponse(DiscoveryNode node, boolean success, String message) {
        super(node);
        this.success = success;
        this.message = message;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(success);
        out.writeString(message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
