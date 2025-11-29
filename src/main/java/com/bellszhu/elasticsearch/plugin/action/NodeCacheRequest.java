package com.bellszhu.elasticsearch.plugin.action;

//import org.elasticsearch.action.support.nodes.BaseNodeRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.TransportRequest;

import java.io.IOException;

/**
 * 单个节点的缓存请求
 */
public class NodeCacheRequest extends TransportRequest {

    private String key;
    private String value;

    // 默认构造函数（必需）
    public NodeCacheRequest() {
        super();
    }

    public NodeCacheRequest(StreamInput in) throws IOException {
        super(in);
        this.key = in.readString();
        this.value = in.readString();
    }

    public NodeCacheRequest(BroadcastCacheRequest request) {
        this.key = request.getKey();
        this.value = request.getValue();
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(key);
        out.writeString(value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
