package org.zerock.finance_dwpj1.service.stock;

import org.springframework.stereotype.Component;


@Component
public class StockGradeCalculatorService {

    public String calculate(Long amount, Double percent, String type) {

        // 실수익
        if ("A".equals(type)) {
            if (amount >= 20000000) return "4";
            if (amount >= 10000000) return "3";
            if (amount >= 0) return "2";
            return "1";   // ← 여기 기존에 0이었는데 1이 더 자연스러움
        }

        // 주식 %
        if ("B".equals(type)) {
            if (percent >= 30) return "A";
            if (percent >= 15) return "B";
            if (percent >= 0) return "C";
            return "D";
        }

        return "NONE";
    }


    // ★ 이모지 변환 통합
    public String gradeToEmoji(String grade) {
        if (grade == null) return "";

        grade = grade.trim().toUpperCase();

        return switch (grade) {
            case "4" -> "💰";
            case "3" -> "💸";
            case "2" -> "💵";
            case "1" -> "💀";
            case "A" -> "💎";
            case "B" -> "🏅";
            case "C" -> "🥈";
            case "D" -> "🐄";
            default -> "";
        };
    }
}