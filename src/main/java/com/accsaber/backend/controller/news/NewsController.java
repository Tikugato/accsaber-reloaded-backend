package com.accsaber.backend.controller.news;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.news.PublicNewsResponse;
import com.accsaber.backend.model.entity.news.NewsType;
import com.accsaber.backend.service.news.NewsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
@Tag(name = "Platform")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "List the news posts", description = "Published posts with anything pinned at the top and the rest "
            + "newest first. Filter with type if you only care about one sort, which is one of BATCH, CAMPAIGN, MILESTONE_SET, "
            + "CURVE or GENERAL. Drafts never appear here.")
    @GetMapping
    public ResponseEntity<Page<PublicNewsResponse>> list(
            @RequestParam(required = false) NewsType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsService.findPublic(type, pageable));
    }

    @Operation(summary = "Get one news post", description = "A single published post. You can address it either by its id or "
            + "by its slug, the readable version of the title, so whichever you happen to have works.")
    @GetMapping("/{idOrSlug}")
    public ResponseEntity<PublicNewsResponse> get(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(newsService.findPublic(idOrSlug));
    }
}
