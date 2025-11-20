package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.insights.TwitterDTO;
import org.zerock.finance_dwpj1.service.common.GPTService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class TwitterService {

    private final GPTService gptService;

    public List<TwitterDTO> getTwitterInsights() {
        // 실제로는 Twitter API를 사용하여 트윗을 가져와야 하지만,
        // 현재는 샘플 데이터를 사용합니다
        List<TwitterDTO> tweets = getSampleTweets();

        // 각 트윗을 GPT API로 번역
        for (TwitterDTO tweet : tweets) {
            if (tweet.getOriginalText() != null && !tweet.getOriginalText().isEmpty()) {
                String translated = gptService.translateTweet(tweet.getOriginalText());
                tweet.setTranslatedText(translated);
            }
        }

        return tweets;
    }

    private List<TwitterDTO> getSampleTweets() {
        List<TwitterDTO> tweets = new ArrayList<>();

        tweets.add(TwitterDTO.builder()
                .name("Cathie Wood")
                .handle("CathieDWood")
                .avatar("https://pbs.twimg.com/profile_images/1356372753708810241/9TtX1Y0R_400x400.jpg")
                .verified(true)
                .originalText("Innovation is accelerating at an unprecedented pace. AI, blockchain, and genomics are converging to reshape our economy. The opportunities are immense for those who can see beyond short-term volatility.")
                .translatedText("혁신이 전례 없는 속도로 가속화되고 있습니다. AI, 블록체인, 유전체학이 융합하여 경제를 재편하고 있습니다. 단기 변동성 너머를 볼 수 있는 사람들에게는 엄청난 기회가 있습니다.")
                .date("2시간 전")
                .url("https://twitter.com/CathieDWood")
                .build());

        tweets.add(TwitterDTO.builder()
                .name("Michael Burry")
                .handle("michaeljburry")
                .avatar("https://pbs.twimg.com/profile_images/1461546042581856257/h8Xq3E3__400x400.jpg")
                .verified(true)
                .originalText("Market valuations are stretched. History shows that periods of excessive optimism are often followed by sharp corrections. Risk management is paramount.")
                .translatedText("시장 밸류에이션이 과도하게 늘어났습니다. 역사는 과도한 낙관주의가 급격한 조정으로 이어지는 경우가 많음을 보여줍니다. 리스크 관리가 무엇보다 중요합니다.")
                .date("5시간 전")
                .url("https://twitter.com/michaeljburry")
                .build());

        tweets.add(TwitterDTO.builder()
                .name("Bill Ackman")
                .handle("BillAckman")
                .avatar("https://pbs.twimg.com/profile_images/1590086977031352320/cFWULKEd_400x400.jpg")
                .verified(true)
                .originalText("Quality businesses with strong moats deserve premium valuations. Focus on companies that can compound earnings over decades, not quarters.")
                .translatedText("강력한 해자를 가진 우량 기업은 프리미엄 밸류에이션을 받을 자격이 있습니다. 분기가 아닌 수십 년에 걸쳐 수익을 복리로 늘릴 수 있는 기업에 집중하세요.")
                .date("1일 전")
                .url("https://twitter.com/BillAckman")
                .build());

        tweets.add(TwitterDTO.builder()
                .name("Ray Dalio")
                .handle("RayDalio")
                .avatar("https://pbs.twimg.com/profile_images/1339006396365189120/--ws4_8r_400x400.jpg")
                .verified(true)
                .originalText("Diversification is the only free lunch in investing. Understanding economic cycles and balancing your portfolio accordingly is crucial for long-term success.")
                .translatedText("분산투자는 투자에서 유일한 공짜 점심입니다. 경제 사이클을 이해하고 그에 따라 포트폴리오를 균형있게 조정하는 것이 장기적 성공에 필수적입니다.")
                .date("2일 전")
                .url("https://twitter.com/RayDalio")
                .build());

        tweets.add(TwitterDTO.builder()
                .name("Howard Marks")
                .handle("howardmarksbook")
                .avatar("https://pbs.twimg.com/profile_images/1357383068502441984/Hx0gCOd-_400x400.jpg")
                .verified(true)
                .originalText("The biggest investment mistakes come from failing to understand where we are in the cycle. Knowing when to be aggressive and when to be defensive is key.")
                .translatedText("가장 큰 투자 실수는 우리가 사이클의 어디에 있는지 이해하지 못하는 데서 비롯됩니다. 언제 공격적이어야 하고 언제 방어적이어야 하는지 아는 것이 핵심입니다.")
                .date("3일 전")
                .url("https://twitter.com/howardmarksbook")
                .build());

        return tweets;
    }
}