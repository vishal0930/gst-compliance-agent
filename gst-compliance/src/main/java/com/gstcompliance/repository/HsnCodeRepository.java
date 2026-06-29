package com.gstcompliance.repository;

import com.gstcompliance.model.HsnCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HsnCodeRepository extends JpaRepository<HsnCode, UUID> {

    Optional<HsnCode> findByHsnCode(String hsnCode);

    @Query(value = """
        SELECT * FROM hsn_codes 
        WHERE description ILIKE CONCAT('%', :query, '%')
        LIMIT :limit
        """, nativeQuery = true)
    List<HsnCode> searchByDescription(@Param("query") String query, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM hsn_codes 
        WHERE embedding IS NOT NULL 
        ORDER BY embedding <-> CAST(:embedding AS vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<HsnCode> findNearestByEmbedding(@Param("embedding") String embedding, @Param("limit") int limit);

    long countByEmbeddingIsNotNull();
}