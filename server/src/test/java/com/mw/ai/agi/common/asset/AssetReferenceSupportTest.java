package com.mw.ai.agi.common.asset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetReferenceSupportTest {

    @Test
    void acceptsTypicalBotAndKnowledgeBaseIds() {
        String botId = "bot_" + UUID.randomUUID();
        String kbId = "kb_" + UUID.randomUUID();

        assertThat(AssetReferenceSupport.requireAssetId(botId)).isEqualTo(botId);
        assertThat(AssetReferenceSupport.requireAssetIds(List.of(botId, kbId))).containsExactly(botId, kbId);
    }

    @Test
    void rejectsOverlongAssetId() {
        String overlong = "bot_" + "a".repeat(AssetReferenceSupport.MAX_ASSET_ID_LENGTH);

        assertThatThrownBy(() -> AssetReferenceSupport.requireAssetId(overlong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(AssetReferenceSupport.MAX_ASSET_ID_LENGTH));
    }

    @Test
    void deduplicatesManyAssetIds() {
        String kbId = "kb_" + UUID.randomUUID();
        List<String> ids = IntStream.range(0, 50)
                .mapToObj(index -> index % 2 == 0 ? kbId : "kb_" + UUID.randomUUID())
                .toList();

        assertThat(AssetReferenceSupport.requireAssetIds(ids)).hasSize(26);
    }
}
