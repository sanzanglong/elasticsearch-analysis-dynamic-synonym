package com.bellszhu.elasticsearch.plugin.search.sort;

import com.bellszhu.elasticsearch.plugin.action.CustomCachePlugin;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LeafFieldComparator;

public class MySortFieldComparator extends FieldComparator<Integer> {

    private final int[] values;
    private int bottomValue;
    private final String field;

    public MySortFieldComparator(int numHits, String field) {
        this.values = new int[numHits];
        this.field = field;
    }

    @Override
    public int compare(int slot1, int slot2) {

        return Integer.compare(values[slot1], values[slot2]);
    }

    @Override
    public void setTopValue(Integer value) {}

    @Override
    public Integer value(int slot) {
        return values[slot];
    }

    @Override
    public LeafFieldComparator getLeafComparator(LeafReaderContext context) {
        return new MyLeafComparator(context);
    }

    private class MyLeafComparator implements LeafFieldComparator {

        private final LeafReaderContext ctx;

        MyLeafComparator(LeafReaderContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void setScorer(org.apache.lucene.search.Scorable scorer) {}

        @Override
        public int compareBottom(int doc) {
            int v = computeValue(doc);
            return Integer.compare(bottomValue, v);
        }

        @Override
        public void setBottom(int slot) {

            bottomValue = values[slot];
        }

        @Override
        public void copy(int slot, int doc) {

            values[slot] = computeValue(doc);
        }

        @Override
        public int compareTop(int doc) {
            return compareBottom(doc);
        }

        private int computeValue(int doc) {

//            return Integer.parseInt(CustomCachePlugin.getCachedValue("my_custom_data"));
//            return doc * Integer.parseInt(CustomCachePlugin.getCachedValue("my_custom_data"));
            return doc > 0 ? 10 : 0;
        }
    }
}
