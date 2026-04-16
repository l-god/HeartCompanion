package com.yupi.springai.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.ai.chat.messages.AssistantMessage;

public class Lanch4jinvoke {
    public static void main(String[] args) {
        ChatLanguageModel qwenChatModel= QwenChatModel.builder()
                .apiKey(Apikey.API_KEY)
                .modelName("qwen-max")
                .build();
        String chat = qwenChatModel.chat("我是神！！");
        System.out.println(chat);
    }
}
