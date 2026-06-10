package com.mw.ai.agi.generation.service;

import java.util.List;
import java.util.Map;

public interface ArchiveCorpusService {
    List<Map<String, Object>> listThemeLibraries();

    List<Map<String, Object>> listThemeLibraryItems(String themeLibraryId);

    Map<String, Object> getArchiveItem(String archiveItemId);

    List<Map<String, Object>> loadCorpusItems(String themeLibraryId);
}
