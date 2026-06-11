package com.mw.ai.agi.system.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.system.persistence.DataDictionaryEntity;
import com.mw.ai.agi.system.persistence.DataDictionaryItemEntity;
import com.mw.ai.agi.system.service.DataDictionaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dictionaries")
public class DataDictionaryController {
    private final DataDictionaryService dictionaryService;

    public DataDictionaryController(DataDictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DataDictionaryEntity>> list() {
        List<DataDictionaryEntity> items = dictionaryService.listDictionaries();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<DataDictionaryEntity> get(@PathVariable String id) {
        return ApiResponse.success(dictionaryService.getDictionary(id));
    }

    @GetMapping("/code/{code}/items")
    public ApiResponse<PageResponse<DataDictionaryItemEntity>> listByCode(@PathVariable String code) {
        List<DataDictionaryItemEntity> items = dictionaryService.listItemsByCode(code);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{id}/items")
    public ApiResponse<PageResponse<DataDictionaryItemEntity>> listItems(@PathVariable String id) {
        List<DataDictionaryItemEntity> items = dictionaryService.listItems(id);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping
    public ApiResponse<DataDictionaryEntity> create(@Valid @RequestBody SaveDictionaryRequest request) {
        return ApiResponse.success(dictionaryService.createDictionary(
                request.code(),
                request.name(),
                request.description(),
                request.status()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<DataDictionaryEntity> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateDictionaryRequest request
    ) {
        return ApiResponse.success(dictionaryService.updateDictionary(
                id,
                request.name(),
                request.description(),
                request.status()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        dictionaryService.deleteDictionary(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/items")
    public ApiResponse<DataDictionaryItemEntity> createItem(
            @PathVariable String id,
            @Valid @RequestBody SaveDictionaryItemRequest request
    ) {
        return ApiResponse.success(dictionaryService.createItem(
                id,
                request.label(),
                request.value(),
                request.description(),
                request.sortOrder(),
                request.status()
        ));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<DataDictionaryItemEntity> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody SaveDictionaryItemRequest request
    ) {
        return ApiResponse.success(dictionaryService.updateItem(
                itemId,
                request.label(),
                request.value(),
                request.description(),
                request.sortOrder(),
                request.status()
        ));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable String itemId) {
        dictionaryService.deleteItem(itemId);
        return ApiResponse.success(null);
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record SaveDictionaryRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            String status
    ) {
    }

    public record UpdateDictionaryRequest(
            @NotBlank String name,
            String description,
            String status
    ) {
    }

    public record SaveDictionaryItemRequest(
            @NotBlank String label,
            @NotBlank String value,
            String description,
            Integer sortOrder,
            String status
    ) {
    }
}
