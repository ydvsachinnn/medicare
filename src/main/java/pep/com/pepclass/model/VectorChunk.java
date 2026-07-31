package pep.com.pepclass.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "vector_chunks")
public class VectorChunk {

    @Id
    private String id;

    private String content;

    private String sourceFile;

    private Integer chunkIndex;

    private List<Double> embedding;

    private LocalDateTime createdAt = LocalDateTime.now();

    public VectorChunk() {
    }

    public VectorChunk(String content, String sourceFile, Integer chunkIndex, List<Double> embedding) {
        this.content = content;
        this.sourceFile = sourceFile;
        this.chunkIndex = chunkIndex;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
