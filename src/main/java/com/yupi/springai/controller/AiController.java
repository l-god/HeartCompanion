package com.yupi.springai.controller;

import com.yupi.springai.agent.YuManus;
import com.yupi.springai.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;
   //同步调用恋爱大师
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }
   //流式调用就是一个一个输出
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    @GetMapping("/love_app/chat/rag")
    public String doChatWithLoveAppRag(String message, String chatId) {
        return loveApp.doChatWithRag(message, chatId);
    }

    @GetMapping("/love_app/chat/tools")
    public String doChatWithLoveAppTools(String message, String chatId) {
        return loveApp.doChatWithTools(message, chatId);
    }

    @GetMapping("/love_app/chat/mcp")
    public String doChatWithLoveAppMcp(String message, String chatId) {
        return loveApp.doChatWithMcp(message, chatId);
    }

    @GetMapping("/love_app/report")
    public Object doChatWithLoveAppReport(String message, String chatId) {
        return loveApp.doChatWithReport(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(String message) {
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }

    @GetMapping("/manus/chat/sync")
    public String doChatWithManusSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    @GetMapping("/manus/chat/rag")
    public String doChatWithManusRag(String message, String chatId) {
        return loveApp.doChatWithRag(message, chatId);
    }

    @GetMapping("/manus/chat/tools")
    public String doChatWithManusTools(String message, String chatId) {
        return loveApp.doChatWithTools(message, chatId);
    }

    @GetMapping("/manus/chat/mcp")
    public String doChatWithManusMcp(String message, String chatId) {
        return loveApp.doChatWithMcp(message, chatId);
    }

    @GetMapping("/manus/report")
    public Object doChatWithManusReport(String message, String chatId) {
        return loveApp.doChatWithReport(message, chatId);
    }



}
