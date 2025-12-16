package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.InfoPost;

import java.util.List;
import java.util.Optional;

/**
 * InfoPost Repository
 */
@Repository
public interface InfoPostRepository extends JpaRepository<InfoPost, Long> {

    /**
     * 활성 게시글 목록 조회 (최신순)
     * @return 삭제되지 않은 게시글 목록
     */
    @Query("SELECT DISTINCT p FROM InfoPost p " +
           "LEFT JOIN FETCH p.sections s " +
           "WHERE p.isDeleted = false " +
           "ORDER BY p.createdDate DESC")
    List<InfoPost> findActivePostsWithSections();

    /**
     * ID로 활성 게시글 조회 (섹션 포함)
     * @param id 게시글 ID
     * @return 게시글 (섹션 포함)
     */
    @Query("SELECT p FROM InfoPost p " +
           "LEFT JOIN FETCH p.sections s " +
           "WHERE p.id = :id AND p.isDeleted = false")
    Optional<InfoPost> findActivePostWithSections(Long id);

    /**
     * 모든 게시글 조회 (관리자용, 삭제된 것 포함)
     * @return 전체 게시글 목록
     */
    @Query("SELECT DISTINCT p FROM InfoPost p " +
           "LEFT JOIN FETCH p.sections s " +
           "ORDER BY p.createdDate DESC")
    List<InfoPost> findAllPostsWithSections();
}