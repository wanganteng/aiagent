package com.wang.aiagent.agent;

import com.wang.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class YuManus extends ToolCallAgent {

    public YuManus(ToolCallback[] allTools, ToolCallbackProvider toolCallbackProvider, ChatModel dashscopeChatModel) {
        super(allTools,toolCallbackProvider);
        this.setName("yuManus");
        String SYSTEM_PROMPT = """  
                你名为“YuManus”，是一款全能型的智能AI助手，旨在解决用户提出的任何任务。
                你拥有多种可用工具，可随时调用以高效完成复杂指令。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        //如果您需要更多的细节，请使用“askHuman”工具/函数调用来获取更多用户信息。
        String NEXT_STEP_PROMPT = """  
                根据用户需求，主动选择最合适的工具或工具组合。
                对于复杂的任务，您可以将问题分解，然后逐步使用不同的工具来解决它。
                使用每种工具后，清晰地说明执行结果，并提出下一步的步骤。
                如果您想在任何时刻停止交互，请使用“终止”工具/函数调用。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
