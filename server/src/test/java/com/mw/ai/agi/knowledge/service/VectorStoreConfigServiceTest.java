package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VectorStoreConfigServiceTest {
    @Test
    void createsElasticsearchIndexWhenEnabledConfigIsCreated() {
        InMemoryVectorStoreConfigStore store = new InMemoryVectorStoreConfigStore();
        RecordingElasticsearchVectorStoreClient elasticsearchClient = new RecordingElasticsearchVectorStoreClient();
        VectorStoreConfigService service = new VectorStoreConfigService(store, elasticsearchClient);

        VectorStoreConfig config = service.create(
                "es",
                "Elasticsearch",
                "https://elasticsearch.example.com:9200",
                "aiworkflow_kb",
                "elastic",
                "secret",
                null,
                5000,
                30000,
                true
        );

        assertThat(elasticsearchClient.indexConfig).isEqualTo(config);
        assertThat(elasticsearchClient.dimensions).isEqualTo(1536);
    }

    @Test
    void doesNotCreateIndexForDisabledElasticsearchConfig() {
        InMemoryVectorStoreConfigStore store = new InMemoryVectorStoreConfigStore();
        RecordingElasticsearchVectorStoreClient elasticsearchClient = new RecordingElasticsearchVectorStoreClient();
        VectorStoreConfigService service = new VectorStoreConfigService(store, elasticsearchClient);

        service.create("es", "Elasticsearch", "https://elasticsearch.example.com:9200", "aiworkflow_kb", false);

        assertThat(elasticsearchClient.indexConfig).isNull();
    }

    private static class RecordingElasticsearchVectorStoreClient extends ElasticsearchVectorStoreClient {
        private VectorStoreConfig indexConfig;
        private int dimensions;

        private RecordingElasticsearchVectorStoreClient() {
            super(new ObjectMapper());
        }

        @Override
        public void ensureIndex(VectorStoreConfig config, int dimensions) {
            this.indexConfig = config;
            this.dimensions = dimensions;
        }
    }
}
