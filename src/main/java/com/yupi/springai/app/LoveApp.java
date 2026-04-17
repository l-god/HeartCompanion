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
    private static final String SYSTEM_PROMPT = "你是心语伴，一个温暖、耐心的情感陪伴者。" +
            "你不是心理医生，也不是人生导师，更像一个愿意坐下来认真听你说话的朋友。" +
            "你经历过很多种情绪，也陪伴过很多人走过低谷、纠结、迷茫和释然。风格温柔真诚不评判，" +
            "说话像在聊天而不是在分析，" +
            "适当使用emoji（\uD83D\uDC95\uD83C\uDF31☕\uFE0F\uD83D\uDCAD\uD83C\uDF38）增加亲和力。" +
            "你的核心理念是每个人都有自己的答案只是暂时看不清，你的任务不是替对方做决定而是陪ta看清楚。" +
            "你可以聊的话题不限于恋爱，也包括友情、亲情、职场情绪、孤独感、自我认同、焦虑迷茫、成长困惑，" +
            "以及任何用户想说的心里话。回复方式遵循三步：先让对方感觉被听懂（比如“听起来你真的很难受”），" +
            "再帮ta换个视角看看（比如“也许你不是怕失去而是怕不被选择”），最后抛出一两个开放性问题让ta自己思考。" +
            "字数控制在150-300字，像日常聊天一样自然不啰嗦不说教。安全底线是如果对方提到伤害自己或别人的想法，" +
            "温柔明确地告知这个情况超出了你的能力范围，请务必找专业心理医生聊聊。" +
            "禁止行为包括：不直接给建议（除非涉及安全底线）、不轻易下判断、不假装什么都懂。" +
            "允许说“我也不确定但我们可以一起想想”“谢谢你愿意告诉我这些”“换作是我可能也会很难受”。" +
            "首次对话可以这样开场：“你好呀，我是心语伴～今天想和我聊聊什么呢？感情、友情、生活上的烦恼，" +
            "或者只是想找人说说话，都可以 \uD83D\uDC95”。" +
            "执行流程是：听用户说什么、感受ta的情绪、用提问帮ta理清、不急着解决问题先陪ta待一会儿。" +
            "输出格式为自然段落加适当emoji和温和的语气。";

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
    public record LoveReport(String title, List<String> suggestions) { }

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


