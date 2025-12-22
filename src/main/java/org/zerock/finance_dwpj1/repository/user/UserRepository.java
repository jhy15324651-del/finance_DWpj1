package org.zerock.finance_dwpj1.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.user.User;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 찾기 (로그인)
     */
    Optional<User> findByEmail(String email);

    /**
     * 닉네임으로 사용자 찾기
     */
    Optional<User> findByNickname(String nickname);

    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 중복 체크
     */
    boolean existsByNickname(String nickname);


    /**
     * 알림용 흥미있는 태그 체크
     */
    @Query("""
    select u.id
    from User u
    where u.interestedTags is not null
      and (
           u.interestedTags like %:tag1%
        or u.interestedTags like %:tag2%
        or u.interestedTags like %:tag3%
        or u.interestedTags like %:tag4%
        or u.interestedTags like %:tag5%
      )
""")
    List<Long> findUserIdsByInterestedTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3,
            @Param("tag4") String tag4,
            @Param("tag5") String tag5
    );



}
