package com.aipostman.service;

import com.aipostman.client.WorkerClient;
import com.aipostman.domain.Source;
import com.aipostman.domain.SourceCandidate;
import com.aipostman.repository.SourceCandidateRepository;
import com.aipostman.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SourceCandidateService {

    @Autowired
    private SourceCandidateRepository sourceCandidateRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private WorkerClient workerClient;

    public List<SourceCandidate> getAllCandidates() {
        return sourceCandidateRepository.findAll();
    }

    public List<SourceCandidate> getCandidatesByStatus(String status) {
        return sourceCandidateRepository.findByStatus(status);
    }

    public Optional<SourceCandidate> getCandidateById(Long id) {
        return sourceCandidateRepository.findById(id);
    }

    @Transactional
    public SourceCandidate approveCandidate(Long id) {
        SourceCandidate candidate = sourceCandidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        // 检查是否已存在相同 URL 的源
        if (sourceRepository.findByUrl(candidate.getUrl()) != null) {
            candidate.setStatus("rejected");
            candidate.setReason("Source with this URL already exists");
            return sourceCandidateRepository.save(candidate);
        }

        // 创建新源
        Source source = new Source();
        source.setName(candidate.getUrl()); // 暂时使用 URL 作为名称，后续可以优化
        source.setUrl(candidate.getUrl());
        source.setSourceType(candidate.getSourceType());
        source.setCategory(candidate.getTopic());
        source.setEnabled(true);
        source.setPriority(50);
        source.setLanguage("zh");

        sourceRepository.save(source);

        // 更新候选源状态
        candidate.setStatus("approved");
        return sourceCandidateRepository.save(candidate);
    }

    @Transactional
    public SourceCandidate rejectCandidate(Long id, String reason) {
        SourceCandidate candidate = sourceCandidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        candidate.setStatus("rejected");
        candidate.setReason(reason);
        return sourceCandidateRepository.save(candidate);
    }

    public void saveCandidate(SourceCandidate candidate) {
        sourceCandidateRepository.save(candidate);
    }

    public List<SourceCandidate> searchCandidates(String query) {
        return sourceCandidateRepository.findByQueryContainingOrTopicContaining(query, query);
    }

    @Transactional
    public SourceCandidate autoApproveWithLLM(Long id) {
        SourceCandidate candidate = sourceCandidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
        
        // 检查是否已存在相同 URL 的源
        if (sourceRepository.findByUrl(candidate.getUrl()) != null) {
            candidate.setStatus("rejected");
            candidate.setReason("Source with this URL already exists");
            return sourceCandidateRepository.save(candidate);
        }
        
        // 调用 worker 中的 LLM 评估服务
        WorkerClient.EvaluateCandidateResponse response = workerClient.evaluateSourceCandidate(
                candidate.getUrl(),
                candidate.getTopic(),
                candidate.getSourceType().name(),
                candidate.getConfidence().doubleValue()
        );
        
        if (response != null && response.approve()) {
            // 创建新源
            Source source = new Source();
            source.setName(candidate.getUrl()); // 暂时使用 URL 作为名称，后续可以优化
            source.setUrl(candidate.getUrl());
            source.setSourceType(candidate.getSourceType());
            source.setCategory(candidate.getTopic());
            source.setEnabled(true);
            source.setPriority(50);
            source.setLanguage("zh");
            
            sourceRepository.save(source);
            
            // 更新候选源状态
            candidate.setStatus("approved");
            candidate.setReason("Approved by LLM: " + response.reason());
        } else {
            // 拒绝候选源
            candidate.setStatus("rejected");
            candidate.setReason("Rejected by LLM: " + (response != null ? response.reason() : "Evaluation failed"));
        }
        
        return sourceCandidateRepository.save(candidate);
    }
}