/*
 * Copyright (c) 2019, guanquan.wang@yandex.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bellszhu.elasticsearch.plugin;

//import co.elastic.clients.elasticsearch._types.query_dsl.Query;
//import co.elastic.clients.elasticsearch.core.SearchRequest;

import com.bellszhu.elasticsearch.plugin.search.collector.ExampleRescoreBuilder;
import lombok.extern.slf4j.Slf4j;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

/**
 * Create by guanquan.wang at 2019-09-18 16:55
 */
@Slf4j
public class SearchCollectorPluginTest2 {
    private ElasticsearchClusterRunner runner;
//    private RestHighLevelClient client;

    @Before
    public void setUp() {

        // create runner instance
        runner = new ElasticsearchClusterRunner();
        // create ES nodes
        runner.build(newConfigs()
                        .numOfNode(1) // Create a test node, default number of node is 3.
                        .clusterName("test")
                        .baseHttpPort(9202)
                .pluginTypes("com.bellszhu.elasticsearch.plugin.search.collector.SearchCollectorPlugin, com.bellszhu.elasticsearch.plugin.action.CustomCachePlugin")
        );

//        runner.build(ElasticsearchClusterRunner.newConfigs()
//                .numOfNode(1)
//                .pluginTypes(SearchCollectorPlugin.class.getName(), CacheWriterIngestPlugin.class.getName())
//        );

    }

    @After
    public void tearDown() throws IOException {
        // close runner
        runner.close();
        // delete all files
        runner.clean();
    }

    public void createIndexR(String indexName)  {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.mapping("""
                {
                    "properties": {
                        "id": {"type": "integer"},
                        "name": {"type": "keyword"},
                        "age": {"type": "integer"}
                    }
                }
                """);

        ActionFuture<CreateIndexResponse> resp = runner.client().admin().indices().create(request);
        try {
            resp.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        runner.ensureYellow();
    }

    @Test
    public void testSearchCollector() {
        String indexName = "search";
        // create an index
        createIndexR(indexName);

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", 1);
        doc.put("name", "Tom");
        doc.put("age", 2);
        list.add(doc);


        BulkRequest bulkRequest = new BulkRequest();

        for (Map<String, Object> docMap : list) {
            bulkRequest.add(
                    new IndexRequest(indexName)
                            .source(docMap, XContentType.JSON)
            );
        }

        try {
            runner.client().bulk(bulkRequest).get();
            runner.client()
                    .admin()
                    .indices()
                    .prepareFlush()
                    .setIndices(indexName)   // optional
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
        }
//
//        SearchRequest searchRequest = new SearchRequest(indexName);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        sourceBuilder.query(QueryBuilders.matchAllQuery());
//        searchRequest.source(sourceBuilder);

        Thread t = new Thread(() -> {
            System.out.println("线程中执行的临时代码...");
            // 你的代码

            BufferedWriter writer = null;
            try{
                writer = new BufferedWriter(new FileWriter("/Users/xl/Downloads/search_logs2.txt"));
            }catch (Exception e){
                e.printStackTrace();
            }

            for (int i = 0; i < 100; i++) {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 1. 主查询（随便一个）
                QueryBuilder mainQuery = QueryBuilders.matchAllQuery();

                // 2. 你的自定义 rescore
                ExampleRescoreBuilder rescore = new ExampleRescoreBuilder(10, "age");

                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                        .query(mainQuery)
                        .addRescorer(rescore);

                SearchRequest searchRequest = new SearchRequest(indexName);
                searchRequest.source(sourceBuilder);

                try {
                    Node node = runner.node();
                    node.start();

                    ActionFuture<SearchResponse> future = node.client().search(searchRequest);


                    // 获取 SearchResponse
                    SearchResponse response = future.get(); // 或 future.get();


                    for (SearchHit hit : response.getHits().getHits()) {
                        System.err.println("nodeId:"+node.getNodeEnvironment().nodeId() + " score:" + hit.getScore());
                        writer.write("nodeId:"+node.getNodeEnvironment().nodeId() + " score:" + hit.getScore());
                        writer.newLine();
                        writer.flush();
                        for (Map.Entry<String, Object> entry : hit.getSourceAsMap().entrySet()) {
                            System.out.println(entry.getKey() + " => " + entry.getValue());
                            log.info("{} => {}", entry.getKey(), entry.getValue());

                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
