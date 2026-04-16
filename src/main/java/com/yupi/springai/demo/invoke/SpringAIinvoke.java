package com.yupi.springai.demo.invoke;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class   SpringAIinvoke  implements CommandLineRunner {
    @Autowired
    private ChatModel chatModel;
    public static void main(String[] args) {
    }

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage= chatModel.call(new Prompt("你好，我是神"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
