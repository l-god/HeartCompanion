package com.yupi.springai.app;

import com.yupi.springai.advisor.MyLoggerAdvisor;
import com.yupi.springai.advisor.ReReadingAdvisor;
import com.yupi.springai.chatmemory.FileBasedChatMemory;
import com.yupi.springai.rag.LoveAppRagCustomAdvisorFactory;
import com.yupi.springai.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {
    @Resource
    Advisor loveAppRagCloudAdvisor;
    //基于rag知识库的问答模式，本地向量知识库
    @Resource
    public  VectorStore LoveVectorStore;
    //pgvector向量数据库实现RAG知识库问答
  // @Resource
   // public  VectorStore pgVectorVectorStore;

    //查询重写
    @Resource
    public QueryRewriter queryRewriter;

    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = "你是一位化名“恋恋老师”的资深恋爱心理咨询师，" +
            "拥有10年情感咨询经验，风格温柔、专业、共情力强。核心原则是绝不直接给建议，" +
            "而是通过提问引导用户自己找到答案。首次对话必须先询问用户情感状态（单身/恋爱/已婚），" +
            "并据此切换策略：单身者重点探索脱单阻碍、暗恋心结或分手疗愈；恋爱中者分析关系现状、矛盾根源与沟通模式；" +
            "已婚者则探讨婚姻挑战、需求表达与情感保鲜。每次回复遵循“共情确认→分析视角→1-2个开放性问题”的结构，" +
            "字数控制在150-300字，适当使用emoji（\uD83D\uDC95\uD83C\uDF38\uD83D\uDCAD）增加亲和力。" +
            "遇到暴力、法律或自伤倾向时，明确告知限制并建议寻求专业帮助。你的使命是：让用户在你这里找到属于自己的答案。";

    public LoveApp(ChatModel dashscopeChatModel) {
        //初始化基于文件的内存对话记忆
       // String fileDir = System.getProperty("user.dir")+"/tmp/chatmemory";
        //ChatMemory chatMemory=new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        //多轮对话
                        new MessageChatMemoryAdvisor(chatMemory),
                        //自定义日志
                        new MyLoggerAdvisor()
                        //自定义增强
                        //new ReReadingAdvisor()  自定义再次阅读用户
                )
                .build();
    }
    //创建对话
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    //支持流式调用
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }


    //生成恋爱报告
    record LoveReport(String title, List<String> suggestions) { }

    public LoveReport doChatWithReport(String message, String chatId) {

        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    public String doChatWithRag(String message, String chatId) {
        //查询重写用户提示词
        String rewriteMessage= queryRewriter.doQueryRewrite(message);

            ChatResponse chatResponse=chatClient.prompt().user(rewriteMessage)
                   .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                           .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                   //开启日志
                   .advisors(new MyLoggerAdvisor())
                    //开启Rag本地知识库，基于问答形式
                    .advisors( new QuestionAnswerAdvisor(LoveVectorStore))
                    //基于PGsql向量数据库加载
                   //.advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                    //基于rag检索增强基于云知识库
                  // .advisors(loveAppRagCloudAdvisor)
                    //应用自定义的检索增强器（文档查询器加上下文增强）
//                    .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                            LoveVectorStore, "单身"
//                    ))
                   .call()
                   .chatResponse();
           String  content = chatResponse.getResult().getOutput().getText();
           log.info("content: {}", content);
           return content;

    }
    @Resource
    private ToolCallback[] allTools;
    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    //利用mcp搞得地图服务,搞得地图api炸了，现在配置的百度地图远程连接
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}


