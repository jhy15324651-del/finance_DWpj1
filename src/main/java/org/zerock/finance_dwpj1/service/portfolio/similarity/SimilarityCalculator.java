package org.zerock.finance_dwpj1.service.portfolio.similarity;

import java.util.Map;

/**
 * 포트폴리오 간 유사도를 계산하는 전략 인터페이스
 *
 * <h2>목적</h2>
 * <p>서로 다른 유사도 알고리즘을 전략 패턴으로 제공하여,
 * 런타임에 알고리즘을 교체하거나 비교할 수 있도록 합니다.</p>
 *
 * <h2>왜 인터페이스로 분리했는가?</h2>
 * <ul>
 *   <li><b>전략 패턴</b>: 유사도 알고리즘을 쉽게 교체 가능</li>
 *   <li><b>확장성</b>: 새로운 알고리즘(JSD, Hellinger 등) 추가 용이</li>
 *   <li><b>테스트 용이성</b>: 각 알고리즘을 독립적으로 테스트 가능</li>
 *   <li><b>비교 가능성</b>: 여러 알고리즘 결과를 동시에 비교 가능</li>
 * </ul>
 *
 * <h2>구현체</h2>
 * <ul>
 *   <li>{@link CosineSimilarityCalculator}: 코사인 유사도 (기본 권장)</li>
 *   <li>(향후) JSDSimilarityCalculator: Jensen-Shannon Divergence</li>
 *   <li>(향후) HellingerSimilarityCalculator: Hellinger Distance</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // Spring으로 주입받기
 * @Autowired
 * private SimilarityCalculator similarityCalculator;
 *
 * Map<String, Double> portfolio1 = Map.of("AAPL", 0.4, "MSFT", 0.6);
 * Map<String, Double> portfolio2 = Map.of("AAPL", 0.5, "MSFT", 0.5);
 *
 * double similarity = similarityCalculator.calculate(portfolio1, portfolio2);
 * // 코사인 유사도: ~99.8%
 * }</pre>
 *
 * <h2>알고리즘 선택 가이드</h2>
 * <table border="1">
 *   <tr>
 *     <th>알고리즘</th>
 *     <th>특징</th>
 *     <th>적합한 경우</th>
 *   </tr>
 *   <tr>
 *     <td>Cosine</td>
 *     <td>방향 유사도, 크기 무관</td>
 *     <td>비중 패턴 비교 (기본 권장)</td>
 *   </tr>
 *   <tr>
 *     <td>JSD</td>
 *     <td>확률분포 차이, 대칭적</td>
 *     <td>정규화된 분포 비교</td>
 *   </tr>
 *   <tr>
 *     <td>Hellinger</td>
 *     <td>확률분포 거리, 직관적</td>
 *     <td>거리 기반 비교</td>
 *   </tr>
 * </table>
 *
 * @author finance_dwpj1 team
 * @since 2025-12-15
 */
public interface SimilarityCalculator {

    /**
     * 두 포트폴리오 간의 유사도를 계산합니다.
     *
     * <p><b>입력 가정</b>:</p>
     * <ul>
     *   <li>포트폴리오는 (ticker → weight) 맵</li>
     *   <li>weight는 비율 (합=1 또는 100% 기준)</li>
     *   <li>정규화되어 있어야 정확함 (알고리즘마다 다름)</li>
     * </ul>
     *
     * <p><b>출력</b>:</p>
     * <ul>
     *   <li>유사도 점수 (0~100%)</li>
     *   <li>100 = 완전히 동일</li>
     *   <li>0 = 완전히 다름</li>
     * </ul>
     *
     * @param portfolio1 첫 번째 포트폴리오 (ticker → weight)
     * @param portfolio2 두 번째 포트폴리오 (ticker → weight)
     * @return 유사도 점수 (0~100)
     *
     * @throws IllegalArgumentException portfolio가 null이거나 비어있는 경우
     */
    double calculate(Map<String, Double> portfolio1, Map<String, Double> portfolio2);

    /**
     * 이 유사도 계산기의 이름을 반환합니다.
     *
     * @return 알고리즘 이름 (예: "Cosine Similarity")
     */
    String getName();
}