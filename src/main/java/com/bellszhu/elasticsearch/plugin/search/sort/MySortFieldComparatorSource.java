package com.bellszhu.elasticsearch.plugin.search.sort;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

public class MySortFieldComparatorSource extends FieldComparatorSource {

    private final String field;

    public MySortFieldComparatorSource(String field) {
        this.field = field;
    }

//    @Override
//    public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
//        return new MySortFieldComparator(numHits, field);
//    }

    @Override
    public FieldComparator<?> newComparator(String s, int i, boolean b, boolean b1) {
        return new MySortFieldComparator(i, field);
    }
}
