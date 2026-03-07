package com.wang.aiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {


    /**
    * 执行请求前，改写Prompt
    * */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
        // 读取前面Advisor的共享信息
        Map<String, Object> context = advisedRequest.adviseContext();
        System.out.println("请求类: " + context.get("advisor_name"));
        System.out.println("开始时间: " + context.get("start_time"));

        //dashScope不支持字符替换，需要用原生的
        String processedText = """
            %s
            Read the question again: %s
            """.formatted(advisedRequest.userText(), advisedRequest.userText());
        return AdvisedRequest.from(advisedRequest)
                .userText(processedText)
                .build();

        /*Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());*/

        /*return AdvisedRequest.from(advisedRequest)
                .userText("""
                        {re2_input_query}
                        Read the question again: {re2_input_query}
                        """)
                .userParams(advisedUserParams)
                .build();*/
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
