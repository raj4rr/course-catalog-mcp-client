package com.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/")
public class PromtsTemplatesController {
    ChatClient chatClient;
    private final Resource userMsgResource;
    private final Resource systemMsgResource;

    public PromtsTemplatesController(ChatClient.Builder builder,
                                     @Value("classpath:/prompts/user-message-1.st") Resource userMsgResource, @Value("classpath:/prompts/system-message.st") Resource systemMsgResource, ChatMemory chatMemory
           ,ToolCallbackProvider  toolCallbackProvider   ){
        this.chatClient = builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(toolCallbackProvider).
                defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build(

        );
        this.userMsgResource = userMsgResource;
        this.systemMsgResource = systemMsgResource;
    }
/*
    @GetMapping("/listOfCities")
    List<String> getCityName(String country) {
        ListOutputConverter converter = new ListOutputConverter();
        PromptTemplate promptTemplate = new PromptTemplate(userMsgResource);
        Map<String, Object> vars = Map.of("country", country, "format", converter.getFormat());
        Message message = promptTemplate.createMessage(vars);

        return converter.convert(chatClient.prompt().system(systemMsgResource).messages(message).call().content());
    }
*/
    @PostMapping("/chat")
    ResponseEntity<Output> chat(@RequestBody  Input input,
                                @CookieValue(name = "X-CONV-ID", required = false) String convId) {
        String conversationId = convId == null ? UUID.randomUUID().toString() : convId;
        var response = this.chatClient.prompt()
                .user(input.prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().content();
        ResponseCookie cookie = ResponseCookie.from("X-CONV-ID", conversationId)
                .path("/")
                .maxAge(3600)
                .build();
        
        Output output = new Output(response);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(output);
    }

     record Input(String prompt) {}
    record Output(String content) {
    }
}
