package com.wang.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.wang.aiagent.constant.FileConstant;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.util.Scanner;

public class AskHumanTool {

    @Tool(description = """
         当你需要更多的细节时，请调用这个工具""")
    public String askHuman(@ToolParam(description = """
            要询问用户的明确信息
            """)  String question) {
        try {
            //后续可以调整为非阻塞，直接把ai需要了解的细节问题列出来返回给用户，然用户在后续回答中提供，而不是卡住，效果不好
            System.out.println("智能体需要你的帮助：" + question);
            Scanner scanner = new Scanner(System.in);
            String userAnswer = scanner.nextLine();
            return userAnswer;
        } catch (Exception e) {
            return "Error to ask human: " + e.getMessage();
        }
    }
}
