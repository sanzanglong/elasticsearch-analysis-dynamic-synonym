package com.bellszhu.elasticsearch.plugin.ingest;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.Map;
import java.util.Collections;

public class CacheWriterIngestPlugin extends Plugin implements IngestPlugin {

    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Collections.singletonMap(
                "cache_writer",                                 // ← 在 pipeline 中使用的名字
                (factories, tag, description, config) -> new CacheWriterProcessor(tag, description)
        );
    }

    // 你的 Processor 实现（不变）
    public static class CacheWriterProcessor extends AbstractProcessor {

        protected CacheWriterProcessor(String tag, String description) {
            super(tag, description);
        }

        @Override
        public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
            // 正确方式：从 IngestMetadata 中获取 _id（ES 8.7+ 唯一合法方式）
            Map<String, Object> metadata = ingestDocument.getIngestMetadata();
            String docId = (String) metadata.get(IngestDocument.Metadata.ID.getFieldName());

            // 如果是 reindex 或其他场景可能没有 _id，可以加个判断
            if (docId == null) {
                docId = "auto-generated-" + System.nanoTime();
            }
            String sourceJson = ingestDocument.getSourceAndMetadata().toString();

            // 写入全局缓存
            GlobalCache.getInstance().put(docId, sourceJson);
            System.out.println("[CacheIngest] 写入缓存 _id=" + docId);

            // 可选：给文档加个字段
            ingestDocument.setFieldValue("cached", true);
            ingestDocument.setFieldValue("cached_at", System.currentTimeMillis());

            return ingestDocument;
        }

        @Override
        public String getType() {
            return "cache_writer";   // 必须和 map 的 key 一致
        }
    }
}