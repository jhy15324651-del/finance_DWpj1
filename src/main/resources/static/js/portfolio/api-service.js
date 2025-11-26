/**
 * API 통신 담당 모듈
 */
export class APIService {
    constructor() {
        this.baseUrl = '';
    }

    /**
     * 투자자 비교 데이터 가져오기
     */
    async fetchComparison(investorIds) {
        try {
            const response = await fetch('/api/portfolio/compare', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ investors: investorIds })
            });

            if (!response.ok) {
                throw new Error('비교 데이터를 가져오는데 실패했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('비교 데이터 로딩 오류:', error);
            throw error;
        }
    }

    /**
     * AI 포트폴리오 추천 생성
     */
    async generatePortfolio(investorIds) {
        try {
            const response = await fetch('/api/portfolio/recommend', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ investors: investorIds })
            });

            if (!response.ok) {
                throw new Error('포트폴리오 생성에 실패했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('포트폴리오 생성 오류:', error);
            throw error;
        }
    }

    /**
     * 투자자 검색
     */
    async searchInvestor(investorName) {
        try {
            const response = await fetch('/api/portfolio/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name: investorName })
            });

            if (!response.ok) {
                throw new Error('투자자 정보를 가져올 수 없습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('투자자 검색 오류:', error);
            throw error;
        }
    }
}