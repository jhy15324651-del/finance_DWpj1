package org.zerock.finance_dwpj1.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorComparisonDTO;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchRequest;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchResponse;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioRecommendationResponse;
import org.zerock.finance_dwpj1.service.common.GPTService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Log4j2
public class InvestorComparisonService {

    private final GPTService gptService;

    public List<InvestorComparisonDTO> compareInvestors(List<String> investorIds) {
        List<InvestorComparisonDTO> comparisons = new ArrayList<>();

        for (String investorId : investorIds) {
            InvestorComparisonDTO comparison;

            // 커스텀 투자자 확인 (custom_로 시작하는 ID)
            if (investorId.startsWith("custom_")) {
                comparison = generateCustomInvestorPhilosophy(investorId);
            } else {
                comparison = getInvestorPhilosophy(investorId);
            }

            // GPT API로 인사이트 생성
            String insights = gptService.analyzeInvestorPhilosophy(investorId);
            comparison.setInsights(insights);

            comparisons.add(comparison);
        }

        return comparisons;
    }

    public InvestorSearchResponse searchInvestor(String investorName) {
        // GPT API를 사용하여 투자자 정보 생성
        String gptResponse = gptService.searchInvestorInfo(investorName);

        // GPT 응답 파싱 (간단한 구현)
        return InvestorSearchResponse.builder()
                .name(investorName)
                .style("AI 분석 투자자")
                .description(gptResponse)
                .build();
    }

    public PortfolioRecommendationResponse generatePortfolioRecommendation(List<String> investorIds) {
        log.info("포트폴리오 추천 생성 시작: " + investorIds);

        // GPT API를 사용하여 포트폴리오 추천 생성
        String gptResponse = gptService.generatePortfolioRecommendation(investorIds);

        // GPT 응답을 그대로 반환 (간단한 구현)
        return PortfolioRecommendationResponse.builder()
                .selectedInvestors(investorIds)
                .combinedPhilosophy(gptResponse)
                .recommendations(new ArrayList<>())
                .rationale(gptResponse)
                .riskProfile("GPT가 분석한 포트폴리오입니다.")
                .build();
    }

    private InvestorComparisonDTO generateCustomInvestorPhilosophy(String investorId) {
        // 커스텀 투자자의 경우 GPT로 철학 생성
        return InvestorComparisonDTO.builder()
                .investorId(investorId)
                .philosophy(Arrays.asList(
                        InvestorComparisonDTO.PhilosophyItem.builder()
                                .category("AI 분석 중")
                                .percentage(70)
                                .build(),
                        InvestorComparisonDTO.PhilosophyItem.builder()
                                .category("투자 전략")
                                .percentage(75)
                                .build(),
                        InvestorComparisonDTO.PhilosophyItem.builder()
                                .category("리스크 관리")
                                .percentage(65)
                                .build()
                ))
                .insights("AI가 분석한 투자자입니다.")
                .build();
    }

    private InvestorComparisonDTO getInvestorPhilosophy(String investorId) {
        return switch (investorId) {
            case "wood" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("혁신 기술")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("장기 성장")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("파괴적 혁신")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("고위험 고수익")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("기술 중심")
                                    .percentage(90)
                                    .build()
                    ))
                    .insights("캐시 우드는 AI, 블록체인 등 혁신 기술에 집중 투자합니다.")
                    .build();

            case "soros" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("매크로 투자")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("반사성 이론")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("투기적 접근")
                                    .percentage(75)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("글로벌 시장 분석")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("고위험 고수익")
                                    .percentage(80)
                                    .build()
                    ))
                    .insights("조지 소로스는 글로벌 매크로 경제 동향을 파악하여 대담한 투자를 실행합니다.")
                    .build();

            case "thiel" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("독점 기업")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("기술 혁신")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("벤처 투자")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("장기 성장")
                                    .percentage(80)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("경쟁 우위")
                                    .percentage(90)
                                    .build()
                    ))
                    .insights("피터 틸은 독점적 경쟁력을 가진 기술 기업에 투자합니다.")
                    .build();

            case "fink" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("ESG 투자")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("지속가능성")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("장기 가치")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("리스크 관리")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("분산투자")
                                    .percentage(80)
                                    .build()
                    ))
                    .insights("래리 핑크는 ESG와 지속가능성을 중시하는 장기 투자를 강조합니다.")
                    .build();

            case "buffett" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("가치투자")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("장기보유")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("재무건전성")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("배당선호")
                                    .percentage(70)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("분산투자")
                                    .percentage(60)
                                    .build()
                    ))
                    .insights("워렌 버핏은 내재가치보다 낮은 가격의 우량주를 장기 보유하는 전략을 선호합니다.")
                    .build();

            case "lynch" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("성장주 투자")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("상향식 접근")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("개별기업 분석")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("분산투자")
                                    .percentage(80)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("PEG 비율 중시")
                                    .percentage(75)
                                    .build()
                    ))
                    .insights("피터 린치는 일상에서 발견한 우수한 기업에 투자하는 상향식 접근법을 사용합니다.")
                    .build();

            case "dalio" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("분산투자")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("리스크 패리티")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("매크로 분석")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("경제 사이클 이해")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("자산 배분")
                                    .percentage(90)
                                    .build()
                    ))
                    .insights("레이 달리오는 경제 사이클을 이해하고 리스크를 균등하게 배분하는 전략을 강조합니다.")
                    .build();

            case "graham" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("안전마진")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("가치투자")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("재무제표 분석")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("보수적 접근")
                                    .percentage(85)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("내재가치 중시")
                                    .percentage(90)
                                    .build()
                    ))
                    .insights("벤저민 그레이엄은 가치투자의 아버지로, 안전마진을 확보한 보수적 투자를 강조합니다.")
                    .build();

            case "simons" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("퀀트 모델")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("데이터 분석")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("알고리즘 트레이딩")
                                    .percentage(95)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("수학적 접근")
                                    .percentage(90)
                                    .build(),
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("단기 거래")
                                    .percentage(70)
                                    .build()
                    ))
                    .insights("짐 사이먼스는 수학과 통계를 활용한 퀀트 투자의 선구자입니다.")
                    .build();

            default -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(new ArrayList<>())
                    .insights("알 수 없는 투자자입니다.")
                    .build();
        };
    }
}