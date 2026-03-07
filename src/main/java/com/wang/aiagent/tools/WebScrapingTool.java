package com.wang.aiagent.tools;


import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.springframework.ai.autoconfigure.chat.model.ToolCallingAutoConfiguration;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

public class WebScrapingTool {
    @Tool(description = "Scrape the content of a web page")
    //@Tool还可以加入参数returnDirect=true让调用工具的结果直接返回给用户，不需要再给AI大模型
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            //可以配置ToolContext作为参数系统重的用户数据
            Document doc = Jsoup.connect(url).get();
            return doc.html();
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
