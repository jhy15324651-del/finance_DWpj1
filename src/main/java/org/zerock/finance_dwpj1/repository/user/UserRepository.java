package org.zerock.finance_dwpj1.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.user.User;

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
}
