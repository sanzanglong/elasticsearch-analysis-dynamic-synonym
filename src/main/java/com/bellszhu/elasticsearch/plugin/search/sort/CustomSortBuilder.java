package com.bellszhu.elasticsearch.plugin.search.sort;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.SortField;
import org.elasticsearch.TransportVersion;
import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.IndexNumericFieldData;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.MultiValueMode;
import org.elasticsearch.search.sort.BucketedSort;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortFieldAndFormat;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 自定义排序构建器示例
 * 基于字段名称进行简单排序
 */
public class CustomSortBuilder extends SortBuilder<CustomSortBuilder> {

    public static final String NAME = "my_sort";
    private static final ParseField FIELD_NAME = new ParseField("field");
    
    private final String fieldName;

//    public CustomSortBuilder(){
//        this.fieldName = "age";
//    }
//
//    public CustomSortBuilder(String fieldName) {
//        this.fieldName = fieldName;
//    }

    public CustomSortBuilder(StreamInput in) throws IOException {
        this.fieldName = "age";
//        this.order(in.readBoolean() ? SortOrder.DESC : SortOrder.ASC);
        this.order(SortOrder.DESC);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(fieldName);
        out.writeBoolean(order() == SortOrder.DESC);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startObject(NAME);
        builder.field(FIELD_NAME.getPreferredName(), fieldName);
        if (order() != null) {
            builder.field(ORDER_FIELD.getPreferredName(), order());
        }
        builder.endObject();
        builder.endObject();
        return builder;
    }

    @Override
    public SortFieldAndFormat build(SearchExecutionContext context) throws IOException {

//        MappedFieldType fieldType = context.getFieldType(this.fieldName);
//        IndexFieldData<?> fieldData = context.getForField(fieldType, MappedFieldType.FielddataOperation.SEARCH);
//
//        SortField field = fieldData.sortField(null, MultiValueMode.MIN, null, false);
//        DocValueFormat formatter = fieldType.docValueFormat(null, (ZoneId)null);
//        return new SortFieldAndFormat(field, formatter);

                boolean reverse = order == SortOrder.DESC;

        SortField sortField = new SortField(
                "age",
                new MySortFieldComparatorSource("age"),
                reverse
        );

        MappedFieldType fieldType = context.getFieldType(this.fieldName);
        DocValueFormat formatter = fieldType.docValueFormat(null, (ZoneId)null);

        return new SortFieldAndFormat(sortField, formatter);



//        DocValueFormat formatter = fieldType.docValueFormat(null, (ZoneId)null);
//        SortField sortField = new SortField(fieldName, SortField.Type.INT, order() == SortOrder.DESC);
//        return new SortFieldAndFormat(sortField, DocValueFormat.BINARY);
    }


    @Override
    public BucketedSort buildBucketedSort(SearchExecutionContext context, BigArrays bigArrays, int bucketSize, BucketedSort.ExtraData extra) throws IOException {
        return new BucketedSort.ForFloats(bigArrays, this.order, DocValueFormat.RAW, bucketSize, extra) {
            public boolean needsScores() {
                return true;
            }

            public Leaf forLeaf(LeafReaderContext ctx) throws IOException {
                return new Leaf(ctx) {
                    private Scorable scorer;
                    private float score;

                    public void setScorer(Scorable scorer) {
                        this.scorer = scorer;
                    }

                    protected boolean advanceExact(int doc) throws IOException {
                        assert doc == this.scorer.docID() : "expected scorer to be on [" + doc + "] but was on [" + this.scorer.docID() + "]";

                        this.score = this.scorer.score();
                        return true;
                    }

                    protected float docValue() {
                        return this.score;
                    }
                };
            }
        };
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        return Version.V_8_7_1.transportVersion;
    }

    @Override
    public CustomSortBuilder rewrite(QueryRewriteContext ctx) throws IOException {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        CustomSortBuilder that = (CustomSortBuilder) obj;
        return Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fieldName);
    }

    // XContent 解析器
    private static final ObjectParser<CustomSortBuilder, Void> PARSER = new ObjectParser<>(NAME);

//    static {
//        PARSER.declareString(CustomSortBuilder::new, FIELD_NAME);
//        declareOrderField(PARSER);
//    }

    public static CustomSortBuilder fromXContentX(XContentParser parser) throws IOException {
        String field = null;
        SortOrder order = SortOrder.DESC;

        if (parser.currentToken() == null) parser.nextToken();
        parser.nextToken(); // {

        while (parser.currentToken() != XContentParser.Token.END_OBJECT) {
            String name = parser.currentName();
            parser.nextToken();

            switch (name) {
                case "field":
                    field = parser.text();
                    break;
                case "order":
                    order = SortOrder.fromString(parser.text());
                    break;
            }

            parser.nextToken();
        }
        StreamInput input = new InputStreamStreamInput(
                new ByteArrayInputStream(field.getBytes(StandardCharsets.UTF_8))
        );
        CustomSortBuilder builder = new CustomSortBuilder(input);
        builder.order = order;
        return builder;
    }
}
