package com.aipostman.service;

import com.aipostman.domain.Source;
import com.aipostman.dto.request.CreateSourceRequest;
import com.aipostman.dto.response.SourceResponse;
import com.aipostman.common.enums.SourceType;
import com.aipostman.repository.SourceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public SourceResponse create(CreateSourceRequest request) {
        return sourceRepository.findByUrl(request.url())
                .map(this::toResponse)
                .orElseGet(() -> {
                    Source source = new Source();
                    source.setName(request.name());
                    source.setUrl(request.url());
                    source.setSourceType(request.sourceType());
                    source.setCategory(request.category());
                    source.setPriority(request.priority() == null ? 50 : request.priority());
                    source.setLanguage(request.language() == null || request.language().isBlank() ? "zh" : request.language());
                    return toResponse(sourceRepository.save(source));
                });
    }

    public List<SourceResponse> list() {
        return sourceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SourceResponse toggle(Long sourceId, boolean enabled) {
        Source source = getSource(sourceId);
        source.setEnabled(enabled);
        return toResponse(sourceRepository.save(source));
    }

    public List<Source> getEnabledSources(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return sourceRepository.findAllByEnabledTrueOrderByPriorityDesc();
        }
        return sourceRepository.findAllById(sourceIds);
    }

    public Source getSource(Long sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));
    }

    @Transactional
    public int seedDefaultSources() {
        List<CreateSourceRequest> defaults = List.of(
                new CreateSourceRequest("GitHub Blog", "https://github.blog/feed/", SourceType.RSS, "tech", 90, "en"),
                new CreateSourceRequest("GitHub Changelog", "https://github.blog/changelog/feed/", SourceType.RSS, "tech", 90, "en"),
                new CreateSourceRequest("InfoQ", "https://feed.infoq.com/", SourceType.RSS, "tech", 85, "en"),
                new CreateSourceRequest("Hacker News Frontpage", "https://hnrss.org/frontpage", SourceType.RSS, "tech", 80, "en"),
                new CreateSourceRequest("TechCrunch", "https://techcrunch.com/feed/", SourceType.RSS, "tech", 84, "en"),
                new CreateSourceRequest("The Verge", "https://www.theverge.com/rss/index.xml", SourceType.RSS, "tech", 78, "en"),
                new CreateSourceRequest("Wired", "https://www.wired.com/feed/rss", SourceType.RSS, "tech", 78, "en"),
                new CreateSourceRequest("MIT Technology Review", "https://www.technologyreview.com/feed/", SourceType.RSS, "tech", 82, "en"),
                new CreateSourceRequest("Cloudflare Blog", "https://blog.cloudflare.com/rss/", SourceType.RSS, "tech", 82, "en"),
                new CreateSourceRequest("AWS ML Blog", "https://aws.amazon.com/blogs/machine-learning/feed/", SourceType.RSS, "ai", 83, "en"),
                new CreateSourceRequest("Hugging Face Blog", "https://huggingface.co/blog/feed.xml", SourceType.RSS, "ai", 86, "en"),
                new CreateSourceRequest("arXiv cs.LG", "https://export.arxiv.org/rss/cs.LG", SourceType.RSS, "ai", 88, "en"),
                new CreateSourceRequest("PyTorch Releases", "https://github.com/pytorch/pytorch/releases.atom", SourceType.ATOM, "ai", 84, "en"),
                new CreateSourceRequest("TensorFlow Releases", "https://github.com/tensorflow/tensorflow/releases.atom", SourceType.ATOM, "ai", 82, "en"),
                new CreateSourceRequest("机器之心", "https://www.jiqizhixin.com/rss", SourceType.RSS, "ai", 88, "zh"),
                new CreateSourceRequest("量子位", "https://www.qbitai.com/feed", SourceType.RSS, "ai", 86, "zh"),
                new CreateSourceRequest("少数派", "https://sspai.com/feed", SourceType.RSS, "tech", 78, "zh"),
                new CreateSourceRequest("爱范儿", "https://www.ifanr.com/feed", SourceType.RSS, "tech", 80, "zh"),
                new CreateSourceRequest("36氪", "https://36kr.com/feed", SourceType.RSS, "tech", 76, "zh"),
                new CreateSourceRequest("掘金后端", "https://juejin.cn/rss/backend", SourceType.RSS, "dev", 78, "zh"),
                new CreateSourceRequest("掘金AI", "https://juejin.cn/rss/ai", SourceType.RSS, "ai", 80, "zh"),
                new CreateSourceRequest("SegmentFault 思否", "https://segmentfault.com/feeds/blogs", SourceType.RSS, "dev", 74, "zh")
        );
        List<Source> created = new ArrayList<>();
        for (CreateSourceRequest item : defaults) {
            if (sourceRepository.findByUrl(item.url()).isPresent()) {
                continue;
            }
            Source source = new Source();
            source.setName(item.name());
            source.setUrl(item.url());
            source.setSourceType(item.sourceType());
            source.setCategory(item.category());
            source.setPriority(item.priority() == null ? 50 : item.priority());
            source.setLanguage(item.language() == null || item.language().isBlank() ? "zh" : item.language());
            created.add(source);
        }
        if (created.isEmpty()) {
            return 0;
        }
        sourceRepository.saveAll(created);
        return created.size();
    }

    private SourceResponse toResponse(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getName(),
                source.getUrl(),
                source.getSourceType(),
                source.getCategory(),
                source.isEnabled(),
                source.getPriority(),
                source.getLanguage()
        );
    }
}
