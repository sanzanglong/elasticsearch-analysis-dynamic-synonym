package com.bellszhu.elasticsearch.plugin.search.collector;

import com.bellszhu.elasticsearch.plugin.synonym.analysis.DynamicSynonymGraphTokenFilterFactory;
import com.bellszhu.elasticsearch.plugin.synonym.analysis.DynamicSynonymTokenFilterFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.elasticsearch.plugins.AnalysisPlugin.requiresAnalysisSettings;


/**
 * @author bellszhu
 */
public class SearchCollectorPlugin extends Plugin implements SearchPlugin {

    @Override
    public List<RescorerSpec<?>> getRescorers() {
        return singletonList(
                new RescorerSpec<>(ExampleRescoreBuilder.NAME, ExampleRescoreBuilder::new, ExampleRescoreBuilder::fromXContent));
    }


}