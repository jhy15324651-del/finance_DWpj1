package com.example.demo.service;

import com.example.demo.model.Newsletter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsletterService {

    public List<Newsletter> getDummyNewsletters() {

        List<Newsletter> list = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Newsletter n = new Newsletter();
            n.setId((long) i);
            n.setTitle("뉴스레터 제목 " + i);
            n.setSummary("이것은 뉴스레터 요약 내용입니다. " + i + "번 뉴스레터의 간단한 설명이 들어갑니다.");
            n.setCreatedDate(LocalDate.now().minusDays(i));

            list.add(n);
        }

        return list;
    }
}
