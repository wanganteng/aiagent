package com.wang.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 自定义日志 Advisor
 * aroundCall是同步,对应.call()和aroundStream流式，对应.stream()
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本(虽然SpringAi内置了SimpleLoggerAdvisor日志拦截器，但是是Debug级别输出，而Spring默认日志级别是info，所以自己定义一个最好)
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        //多个Advisor执行顺序
        return 0;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("AI Request: {}", request.userText());

        //上下文共享，在多个Advisor或者单个Advisor的前置后置中共享
        request = request.updateContext(context -> {
            context.put("advisor_name", this.getClass().getSimpleName());
            context.put("start_time", System.currentTimeMillis());
            return context;
        });

        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
    }

    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        //责任链模式，第一个Advisor执行完chain.nextAroundCall(advisedRequest)会阻塞，然后到第二个，第三个，到最后一个，然后调用ai，然后不断返回来执行after
        //类似于栈，before先一直入栈，中间调用ai，最后不断出栈调用after
        advisedRequest = this.before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
