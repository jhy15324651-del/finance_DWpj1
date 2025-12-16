package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.content.ContentInfoSection;

import java.util.List;
import java.util.Optional;

/**
 * ContentInfoSection Repository
 *
 * @deprecated 구조 변경으로 InfoPostRepository 사용을 권장합니다.
 * 섹션은 InfoPost를 통해 조회하세요.
 *
 * 기본 CRUD만 제공하며, 비즈니스 로직은 InfoPostService를 사용하세요.
 */
@Deprecated
public interface ContentInfoSectionRepository extends JpaRepository<ContentInfoSection, Long> {

    /**
     * 섹션 타입으로 조회 (InfoPost와 무관하게 섹션만 조회)
     * 주의: 삭제된 게시글의 섹션도 포함될 수 있습니다.
     * 일반적으로는 InfoPostRepository를 통해 조회하세요.
     */
    Optional<ContentInfoSection> findBySectionType(String sectionType);

    /**
     * 모든 섹션 조회 (표시 순서대로)
     * 주의: 삭제된 게시글의 섹션도 포함될 수 있습니다.
     * 일반적으로는 InfoPostRepository를 통해 조회하세요.
     */
    List<ContentInfoSection> findAllByOrderByDisplayOrder();
}