package org.zerock.finance_dwpj1.entity.insights;

import jakarta.persistence.*;
import lombok.*;

/**
 * 관리자 사용자 엔티티
 * 뉴스 수정/삭제 권한을 가진 관리자 계정
 */
@Entity
@Table(name = "admin_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt로 암호화된 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.ADMIN;

    /**
     * 권한 열거형
     */
    public enum Role {
        ADMIN,  // 관리자
        USER    // 일반 사용자
    }
}
