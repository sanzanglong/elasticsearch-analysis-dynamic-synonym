package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.support.nodes.BaseNodesRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * 广播缓存请求 - 携带缓存的 key 和 value
 */
public class BroadcastCacheRequest extends BaseNodesRequest<BroadcastCacheRequest> {

    private String key;
    private String value;

    // 默认构造函数（必需）
    public BroadcastCacheRequest() {
        super((String[]) null);
    }

    public BroadcastCacheRequest(StreamInput in) throws IOException {
        super(in);
        this.key = in.readString();
        this.value = in.readString();
    }

    public BroadcastCacheRequest(String[] nodesIds, String key, String value) {
        super(nodesIds);
        this.key = key;
        this.value = value;
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
