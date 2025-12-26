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

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.bellszhu.elasticsearch.plugin.search.collector.*;
import com.bellszhu.elasticsearch.plugin.search.sort.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

/**
 * Create by guanquan.wang at 2019-09-18 16:55
 */
@Slf4j
public class SearchSortPluginTest3 {
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
                        .baseHttpPort(9201)
                                .pluginTypes("com.bellszhu.elasticsearch.plugin.search.sort.CustomSortPlugin, com.bellszhu.elasticsearch.plugin.action.CustomCachePlugin")
//                .pluginTypes("com.bellszhu.elasticsearch.plugin.search.collector.SearchCollectorPlugin, com.bellszhu.elasticsearch.plugin.action.CustomCachePlugin")
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

        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("id", 2);
        doc2.put("name", "Tom2");
        doc2.put("age", 2);
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
//            runner.client()
//                    .admin()
//                    .indices()
//                    .prepareFlush()
//                    .setIndices(indexName)   // optional
//                    .get();
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
                writer = new BufferedWriter(new FileWriter("/Users/xl/Downloads/search_logs.txt"));
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
//                QueryBuilder mainQuery = QueryBuilders.matchAllQuery();
                QueryBuilder mainQuery = QueryBuilders.termQuery("name", "Tom");
                        // 2. 你的自定义 rescore
                ExampleRescoreBuilder rescore = new ExampleRescoreBuilder(10, "age");

                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                        .query(mainQuery)
//                        .addRescorer(rescore)
                ;

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


    @Test
    public void testSearchCollectorSort() {
        String indexName = "search";
        // create an index
        createIndexR(indexName);

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", 1);
        doc.put("name", "Tom1");
        doc.put("age", 1);
        list.add(doc);

        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("id", 2);
        doc2.put("name", "Tom2");
        doc2.put("age", 2);
        list.add(doc2);

        Map<String, Object> doc3 = new HashMap<>();
        doc3.put("id", 3);
        doc3.put("name", "Tom3");
        doc3.put("age", 3);
        list.add(doc3);

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
                writer = new BufferedWriter(new FileWriter("/Users/xl/Downloads/search_logs.txt"));
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

//                List<SortBuilder> sortOptions = new ArrayList<>();
//                SortBuilder sort = SortBuilders.scriptSort(sortScript ->
//                        sortScript.type(ScriptSortType.Number)
//                                .order(SortOrder.Desc)
//                                .script(script ->
//                                        script.inline(inline ->
//                                                inline.source("if(params['_source']['province'] == params.province){\n" +
//                                                                "                        1\n" +
//                                                                "                      } else {\n" +
//                                                                "                        0\n" +
//                                                                "                      }")
//                                                        .params("province", JsonData.of("湖北"))
//                                        )
//                                ));


                // 构建 ScriptSortBuilder
                Map<String, Object> params = new HashMap<>();
                params.put("weight", 10);   // 传参数到脚本中

//                Script script = new Script(
//                        ScriptType.INLINE,
//                        "painless",
//                        "if(doc['age'].value == 1){\n" +
//                                "                        1\n" +
//                                "                      } else {\n" +
//                                "                        0\n" +
//                                "                      }",
//                        params
//                );

                Script script = new Script(
                        ScriptType.INLINE,
                        "painless",
                        "if(doc['age'].value == 1){\n" +
                                "                        1\n" +
                                "                      } else {\n" +
                                "                        0\n" +
                                "                      }",
                        params
                );


// 构造排序器
                ScriptSortBuilder sort1 = SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER)
                        .order(org.elasticsearch.search.sort.SortOrder.DESC);     // 排序方向 DESC / ASC

                CustomSortBuilder mySort = null;  // 自定义字段
                try {
                    StreamInput streamInput = StreamInput.wrap("age".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    mySort = new CustomSortBuilder(streamInput);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mySort.order(SortOrder.DESC);

                FieldSortBuilder sort = SortBuilders
                        .fieldSort("id")
                        .order(SortOrder.ASC);


                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                        .query(mainQuery)
//                        .addRescorer(rescore)
                        .sort(mySort)
                        .sort(sort)
                        ;

                SearchRequest searchRequest = new SearchRequest(indexName);
                searchRequest.source(sourceBuilder);

                try {
                    Node node = runner.node();
                    node.start();

                    ActionFuture<SearchResponse> future = node.client().search(searchRequest);


                    // 获取 SearchResponse
                    SearchResponse response = future.get(); // 或 future.get();

                    System.out.println("============");
                    log.info("============");
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
