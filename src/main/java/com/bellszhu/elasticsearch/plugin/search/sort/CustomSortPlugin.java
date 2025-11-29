package com.bellszhu.elasticsearch.plugin.search.sort;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ParseField;

import java.util.List;

public class CustomSortPlugin extends Plugin implements SearchPlugin {
    @Override
    public List<NamedXContentRegistry.Entry> getNamedXContent() {
        return List.of(
                new NamedXContentRegistry.Entry(
                        SortBuilder.class,
                        new ParseField(CustomSortBuilder.NAME),
                        parser -> CustomSortBuilder.fromXContentX(parser)
                )
        );
    }

    @Override
    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return List.of(
                new NamedWriteableRegistry.Entry(
                        SortBuilder.class,
                        CustomSortBuilder.NAME,
                        CustomSortBuilder::new
                )
        );
    }
}
