package org.zerock.finance_dwpj1.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그인 아이디 (이메일)
     */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * 화면 표시용 닉네임
     */
    @Column(unique = true, nullable = false, length = 30)
    private String nickname;

    /**
     * BCrypt 암호화된 비밀번호
     */
    @Column(nullable = false)
    private String password;

    /**
     * 전화번호 (선택)
     */
    @Column(length = 20)
    private String phoneNumber;

    /**
     * 가입일
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 활성화 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 권한 (USER, ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column()
    private String grade;

    /**
     * 알림용 관심 태그 필드
     */
    @Column(length = 500)
    private String interestedTags;

    /**
     * 알림 ON/OFF
     */
    // User.java
    @Column(nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;

}