package pep.com.pepclass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pep.com.pepclass.model.VectorChunk;
import pep.com.pepclass.repository.VectorChunkRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final VectorChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    public VectorSearchService(VectorChunkRepository chunkRepository, EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    public List<VectorChunk> findSimilarChunks(String queryText, int topK, double threshold) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return List.of();
        }

        // 1. Generate embedding vector for the user query
        List<Double> queryVector = embeddingService.getEmbedding(queryText);
        if (queryVector == null || queryVector.isEmpty()) {
            log.warn("[VectorSearch] External embedding API unavailable. Executing keyword search fallback for: '{}'", queryText);
            return findChunksByKeywordSearch(queryText, topK);
        }

        // 2. Fetch all vector chunks from MongoDB
        List<VectorChunk> allChunks = chunkRepository.findAll();
        if (allChunks.isEmpty()) {
            log.warn("[VectorSearch] Chunk registry is empty. No medical records indexed.");
            return List.of();
        }

        List<ChunkMatch> matches = new ArrayList<>();

        // 3. Compute Cosine Similarity for each document segment in memory
        for (VectorChunk chunk : allChunks) {
            List<Double> chunkVector = chunk.getEmbedding();
            if (chunkVector == null || chunkVector.isEmpty()) continue;

            double score = calculateCosineSimilarity(queryVector, chunkVector);
            
            if (score >= threshold) {
                matches.add(new ChunkMatch(chunk, score));
            }
        }

        // 4. Sort matches by score descending
        matches.sort((a, b) -> Double.compare(b.score, a.score));

        // 5. Select top K context segments
        List<VectorChunk> results = new ArrayList<>();
        int limit = Math.min(topK, matches.size());
        for (int i = 0; i < limit; i++) {
            ChunkMatch match = matches.get(i);
            results.add(match.chunk);
            log.info("[VectorSearch] MATCH FOUND in '{}' - Score: {}", match.chunk.getSourceFile(), String.format("%.4f", match.score));
        }

        return results;
    }

    private double calculateCosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size() || vec1.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            double val1 = vec1.get(i);
            double val2 = vec2.get(i);

            dotProduct += val1 * val2;
            normA += val1 * val1;
            normB += val2 * val2;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public List<VectorChunk> findChunksByKeywordSearch(String queryText, int topK) {
        List<VectorChunk> allChunks = chunkRepository.findAll();
        if (allChunks.isEmpty()) return List.of();

        String[] keywords = queryText.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        List<ChunkMatch> matches = new ArrayList<>();

        for (VectorChunk chunk : allChunks) {
            String contentLower = chunk.getContent().toLowerCase();
            String sourceFile = chunk.getSourceFile() != null ? chunk.getSourceFile().toLowerCase() : "";
            int score = 0;
            for (String kw : keywords) {
                if (kw.length() > 2) {
                    if (contentLower.contains(kw)) score += 1;
                    if (sourceFile.contains(kw)) score += 5;
                }
            }
            if (score > 0) {
                matches.add(new ChunkMatch(chunk, score));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score, a.score));

        List<VectorChunk> results = new ArrayList<>();
        int limit = Math.min(topK, matches.size());
        for (int i = 0; i < limit; i++) {
            results.add(matches.get(i).chunk);
            log.info("[KeywordSearch] MATCH FOUND in '{}' - Term Score: {}", matches.get(i).chunk.getSourceFile(), matches.get(i).score);
        }

        return results;
    }

    // Helper static class to hold score associations
    private static class ChunkMatch {
        final VectorChunk chunk;
        final double score;

        ChunkMatch(VectorChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
