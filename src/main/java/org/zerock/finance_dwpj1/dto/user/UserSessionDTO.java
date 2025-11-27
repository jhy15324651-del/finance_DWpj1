package org.zerock.finance_dwpj1.dto.user;

import lombok.*;
import org.zerock.finance_dwpj1.entity.user.Role;

/**
 * 세션에 저장할 사용자 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionDTO {
    private Long userId;
    private String email;
    private String nickname;
    private Role role;
}
