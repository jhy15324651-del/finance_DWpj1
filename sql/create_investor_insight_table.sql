-- 투자자 인사이트 테이블 생성
CREATE TABLE IF NOT EXISTS `investor_insight` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '일련번호',
    `investor_id` VARCHAR(50) NOT NULL UNIQUE COMMENT '투자자 고유 ID (예: wood, soros, thiel, fink)',
    `name` VARCHAR(100) NOT NULL COMMENT '투자자 이름 (한국어)',
    `philosophy_ko` TEXT NOT NULL COMMENT '투자 철학 (한국어)',
    `investment_style` VARCHAR(100) COMMENT '투자 스타일 (예: 가치투자, 성장주 투자)',
    `profile_image_url` VARCHAR(500) COMMENT '프로필 이미지 URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
    `modified_by` VARCHAR(100) COMMENT '관리자 이메일',
    INDEX `idx_investor_id` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='투자자 인사이트 (정적 콘텐츠)';

-- 초기 데이터 INSERT (기존 하드코딩된 인사이트를 DB로 이전)
INSERT INTO `investor_insight` (`investor_id`, `name`, `philosophy_ko`, `investment_style`, `modified_by`)
VALUES
    ('wood', '캐시 우드 (Cathie Wood)', '캐시 우드는 AI, 블록체인, 전기차 등 혁신 기술에 집중 투자합니다. 파괴적 혁신을 추구하며, 장기적인 성장 가능성이 높은 기술 기업을 선호합니다.', '혁신 기술 투자', 'system'),

    ('soros', '조지 소로스 (George Soros)', '조지 소로스는 글로벌 매크로 경제 동향을 파악하여 대담한 투자를 실행합니다. 반사성 이론을 바탕으로 시장의 비합리성을 이용하며, 고위험 고수익 투자를 선호합니다.', '매크로 투자', 'system'),

    ('thiel', '피터 틸 (Peter Thiel)', '피터 틸은 독점적 경쟁력을 가진 기술 기업에 투자합니다. 벤처 캐피탈 투자자로서 혁신적인 스타트업을 발굴하고, 장기적인 성장 잠재력을 중시합니다.', '벤처 투자', 'system'),

    ('fink', '래리 핑크 (Larry Fink)', '래리 핑크는 ESG(환경·사회·지배구조)와 지속가능성을 중시하는 장기 투자를 강조합니다. 리스크 관리와 분산투자를 통해 안정적인 수익을 추구합니다.', 'ESG 투자', 'system'),

    ('buffett', '워렌 버핏 (Warren Buffett)', '워렌 버핏은 내재가치보다 낮은 가격의 우량주를 장기 보유하는 전략을 선호합니다. 재무건전성이 뛰어난 기업을 선별하고, 인내심 있는 가치투자를 실천합니다.', '가치투자', 'system'),

    ('lynch', '피터 린치 (Peter Lynch)', '피터 린치는 일상에서 발견한 우수한 기업에 투자하는 상향식 접근법을 사용합니다. 개별 기업을 철저히 분석하고, PEG 비율을 중시하는 성장주 투자를 선호합니다.', '성장주 투자', 'system'),

    ('dalio', '레이 달리오 (Ray Dalio)', '레이 달리오는 경제 사이클을 이해하고 리스크를 균등하게 배분하는 전략을 강조합니다. 리스크 패리티와 분산투자를 통해 안정적인 수익을 추구하며, 매크로 경제 분석을 중시합니다.', '리스크 패리티', 'system'),

    ('graham', '벤저민 그레이엄 (Benjamin Graham)', '벤저민 그레이엄은 가치투자의 아버지로, 안전마진을 확보한 보수적 투자를 강조합니다. 재무제표 분석을 통해 내재가치를 평가하고, 시장 가격이 내재가치보다 낮을 때 투자합니다.', '가치투자', 'system'),

    ('simons', '짐 사이먼스 (Jim Simons)', '짐 사이먼스는 수학과 통계를 활용한 퀀트 투자의 선구자입니다. 알고리즘 트레이딩과 데이터 분석을 통해 시장의 패턴을 찾아내고, 수학적 모델을 기반으로 투자 결정을 내립니다.', '퀀트 투자', 'system')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `philosophy_ko` = VALUES(`philosophy_ko`),
    `investment_style` = VALUES(`investment_style`),
    `updated_at` = CURRENT_TIMESTAMP;