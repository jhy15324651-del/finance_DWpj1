/**
 * 투자자 데이터 및 선택 관리 모듈
 */
export class InvestorManager {
    constructor() {
        this.selectedInvestors = [];
        this.maxInvestors = 4;
        this.customInvestors = [];

        // 사용 가능한 투자자 목록
        this.availableInvestors = [
            {
                id: 'wood',
                name: '캐시 우드',
                style: '파괴적 혁신 투자',
                description: 'ARK Invest 창립자, AI와 블록체인 등 혁신 기술 중심 투자'
            },
            {
                id: 'soros',
                name: '조지 소로스',
                style: '매크로 투자',
                description: '글로벌 매크로 경제 동향을 활용한 투기적 투자'
            },
            {
                id: 'thiel',
                name: '피터 틸',
                style: '벤처 캐피털',
                description: 'PayPal 공동 창업자, 독점 기업과 기술 혁신 투자'
            },
            {
                id: 'fink',
                name: '래리 핑크',
                style: 'ESG 투자',
                description: 'BlackRock CEO, 지속가능성과 장기 가치 중심 투자'
            },
            {
                id: 'buffett',
                name: '워렌 버핏',
                style: '가치투자',
                description: '장기 가치투자의 대가, 내재가치보다 낮은 가격의 우량주를 선호'
            },
            {
                id: 'lynch',
                name: '피터 린치',
                style: '성장주 투자',
                description: '일상에서 투자 아이디어를 찾는 상향식 접근법'
            },
            {
                id: 'dalio',
                name: '레이 달리오',
                style: '리스크 패리티',
                description: '분산투자와 경제 사이클 이해를 중시하는 매크로 투자'
            },
            {
                id: 'graham',
                name: '벤저민 그레이엄',
                style: '가치투자 원조',
                description: '안전마진을 중시하는 보수적 가치투자'
            },
            {
                id: 'simons',
                name: '짐 사이먼스',
                style: '퀀트 투자',
                description: '수학과 통계를 활용한 알고리즘 투자'
            }
        ];
    }

    /**
     * 기본 투자자 설정
     */
    setDefaultInvestors() {
        this.selectedInvestors = ['wood', 'soros', 'thiel', 'fink'];
    }

    /**
     * 투자자 추가
     */
    addInvestor(investorId) {
        if (this.selectedInvestors.length >= this.maxInvestors) {
            return { success: false, message: '최대 4명까지만 비교할 수 있습니다.' };
        }

        if (this.selectedInvestors.includes(investorId)) {
            return { success: false, message: '이미 선택된 투자자입니다.' };
        }

        this.selectedInvestors.push(investorId);
        return { success: true };
    }

    /**
     * 투자자 제거
     */
    removeInvestor(investorId) {
        this.selectedInvestors = this.selectedInvestors.filter(id => id !== investorId);
        return { success: true };
    }

    /**
     * 커스텀 투자자 추가
     */
    addCustomInvestor(investorData) {
        const customId = 'custom_' + Date.now();
        const customInvestor = {
            id: customId,
            name: investorData.name,
            style: investorData.style || '커스텀 투자자',
            description: investorData.description || '검색으로 추가된 투자자'
        };

        this.customInvestors.push(customInvestor);
        return customInvestor;
    }

    /**
     * ID로 투자자 찾기
     */
    getInvestorById(investorId) {
        let investor = this.availableInvestors.find(inv => inv.id === investorId);
        if (!investor) {
            investor = this.customInvestors.find(inv => inv.id === investorId);
        }
        return investor;
    }

    /**
     * 선택되지 않은 투자자 목록 가져오기
     */
    getAvailableInvestors() {
        return this.availableInvestors.filter(inv => !this.selectedInvestors.includes(inv.id));
    }

    /**
     * 선택된 투자자 목록 가져오기
     */
    getSelectedInvestors() {
        return this.selectedInvestors.map(id => this.getInvestorById(id)).filter(inv => inv);
    }

    /**
     * 투자자 필터링 (검색)
     */
    filterInvestors(searchTerm) {
        if (!searchTerm) {
            return [];
        }

        const term = searchTerm.toLowerCase();
        return this.availableInvestors.filter(investor =>
            investor.name.toLowerCase().includes(term) ||
            investor.style.toLowerCase().includes(term) ||
            investor.description.toLowerCase().includes(term)
        );
    }

    /**
     * 선택 가능한 투자자 수 확인
     */
    canAddMore() {
        return this.selectedInvestors.length < this.maxInvestors;
    }

    /**
     * 포트폴리오 생성 가능 여부
     */
    canGeneratePortfolio() {
        return this.selectedInvestors.length === this.maxInvestors;
    }

    /**
     * 선택된 투자자 수 가져오기
     */
    getSelectedCount() {
        return this.selectedInvestors.length;
    }

    /**
     * 모든 투자자 목록 가져오기
     */
    getAllInvestors() {
        return [...this.availableInvestors, ...this.customInvestors];
    }
}