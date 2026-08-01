package com.medicare.chatbot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.chatbot.model.VectorChunk;
import java.util.List;

@Repository
public interface VectorChunkRepository extends MongoRepository<VectorChunk, String> {
    List<VectorChunk> findBySourceFile(String sourceFile);
    void deleteBySourceFile(String sourceFile);
}
