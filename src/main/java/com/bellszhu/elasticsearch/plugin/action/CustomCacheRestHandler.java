package com.bellszhu.elasticsearch.plugin.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.Table;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.cat.AbstractCatAction;
import org.elasticsearch.rest.action.cat.RestTable;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class CustomCacheRestHandler extends AbstractCatAction {

    private final CustomCachePlugin.NodeCacheService myService;

    public CustomCacheRestHandler(CustomCachePlugin.NodeCacheService myService) {
        this.myService = myService;
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, "/_cat/put/cache"),
                new Route(POST, "/_cat/put/cache"));
    }

    @Override
    public String getName() {
        return "rest_handler_cat_example";
    }

    @Override
    protected RestChannelConsumer doCatRequest(final RestRequest request, final NodeClient client) {
        final String key = request.param("key", "my_custom_data");
        final String value = request.param("value", "123");

        // 1. 先在本地节点设置缓存
        myService.set(key, value);

        // 2. 广播到所有节点（包括当前节点）
        BroadcastCacheRequest broadcastRequest = new BroadcastCacheRequest(
                new String[0],  // 空数组表示发送到所有节点
                key,
                value
        );

        // 4. 构建响应表格，显示所有节点的执行结果
        Table table = getTableWithHeader(request);

        table.startRow();
        table.addCell("node", "desc:Node Name");
        table.addCell("status", "desc:Status");
        table.addCell("message", "desc:" + value);
        table.endRow();

        return channel -> {
            try {

                // 3. 同步执行广播操作
//                BroadcastCacheResponse broadcastResponse = null;  // 同步阻塞等待所有节点响应


                new Thread(() -> {
                    // 3. 同步执行广播操作
                    BroadcastCacheResponse broadcastResponse = client.execute(
                            BroadcastCacheAction.INSTANCE,
                            broadcastRequest
                    ).actionGet();
                }).start();


//
//                // 添加成功的节点信息
//                for (NodeCacheResponse nodeResponse : broadcastResponse.getNodes()) {
//                    table.startRow();
//                    table.addCell(nodeResponse.getNode().getName());
//                    table.addCell(nodeResponse.isSuccess() ? "SUCCESS" : "FAILED");
//                    table.addCell(nodeResponse.getMessage());
//                    table.endRow();
//                }
//
//                // 添加失败的节点信息
//                for (var failure : broadcastResponse.failures()) {
//                    table.startRow();
//                    table.addCell(failure.nodeId());
//                    table.addCell("ERROR");
//                    table.addCell(failure.getMessage());
//                    table.endRow();
//                }
//
//
//                // 如果所有节点都成功了，记录日志
//                if (broadcastResponse.failures().isEmpty()) {
//                    String successNodes = broadcastResponse.getNodes().stream()
//                            .map(r -> r.getNode().getName())
//                            .collect(Collectors.joining(", "));
//                    System.out.println(">>> Cache broadcast SUCCESS to all nodes: " + successNodes);
//                }
                channel.sendResponse(RestTable.buildResponse(table, channel));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void documentation(StringBuilder sb) {
        sb.append(documentation());
    }

    public static String documentation() {
        return "/_cat/example\n";
    }

    @Override
    protected Table getTableWithHeader(RestRequest request) {
        final Table table = new Table();
        table.startHeaders();
        table.addCell("node", "desc:Node Name");
        table.addCell("status", "desc:Status");
        table.addCell("message", "desc:Message");
        table.endHeaders();
        return table;
    }
}
