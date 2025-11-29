package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.ActionType;

/**
 * 自定义 Action - 用于广播缓存更新到所有节点
 */
public class BroadcastCacheAction extends ActionType<BroadcastCacheResponse> {

    public static final BroadcastCacheAction INSTANCE = new BroadcastCacheAction();
    public static final String NAME = "cluster:admin/cache/broadcast";

    private BroadcastCacheAction() {
        super(NAME, BroadcastCacheResponse::new);
    }
}
