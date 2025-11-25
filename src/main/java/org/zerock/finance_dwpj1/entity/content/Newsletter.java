package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

/**
 * 뉴스레터 엔티티
 * 주기적으로 발행되는 뉴스레터를 저장
 */
@Entity
@Table(name = "newsletter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Newsletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
    }
}
