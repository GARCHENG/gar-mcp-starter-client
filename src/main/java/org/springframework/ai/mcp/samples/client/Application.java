/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.mcp.samples.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.samples.client.common.PromptEnums;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.*;

@SpringBootApplication
public class Application {


    public static void main(String[] args) {
        setProxy();
        SpringApplication.run(Application.class, args);
    }

    private static void setProxy() {
        String proxy = "127.0.0.1";  // 如果代理在你本机就127.0.0.1
        int port = 10811;   //设置科学上网代理的端口，
        System.setProperty("proxyType", "4");
        System.setProperty("proxyPort", Integer.toString(port));
        System.setProperty("proxyHost", proxy);
        System.setProperty("proxySet", "true");
    }

    @Value("${ai.user.input}")
    private String userInput;

    private static final Map<String, String> resultMap = new HashMap<>();

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            List<Message> msgLsit = new ArrayList<>();

            setPrompt(msgLsit);

            System.out.println("我:");
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (clearContext(input, msgLsit)) continue;
                if (exit(context, input)) return;
                msgLsit.add(new UserMessage(input));
                new Thread(() -> {
                    try {
                        String content = chatClient.prompt(new Prompt(msgLsit)).call().content();
                        resultMap.put("result", content);
                    } catch (Exception e) {
                        resultMap.put("result", "error:" + e.getMessage());
                    }

                }).start();

                waitResult();
                String content = resultMap.remove("result");
                msgLsit.add(new AssistantMessage(content));// 输出结果
                System.out.println("\rASSISTANT:" + content);
                System.out.println("我:");
            }
        };
    }

    private static void setPrompt(List<Message> msgLsit) {
        System.out.println("please choose a prompt [cursor]");
        String choosePrompt = new Scanner(System.in).nextLine();
        PromptEnums promptEnums = PromptEnums.of(choosePrompt);
        if (null != promptEnums) {
            msgLsit.add(new SystemMessage(promptEnums.getPrompt()));
        }
    }

    private static boolean clearContext(String input, List<Message> msgLsit) {
        if ("/clear".equals(input)) {
            msgLsit.clear();
            System.out.println("context clear success!");
            System.out.println("我:");
            return true;
        }
        return false;
    }

    private static boolean exit(ConfigurableApplicationContext context, String input) {
        if ("exit".equals(input)) {
            System.out.println("bye!");
            context.close();
            return true;
        }
        return false;
    }

    private static void waitResult() throws InterruptedException {
        char[] spinners = {'⠇', '⠋', '⠙', '⠸', '⠴', '⠦', '⠇', '⠋'};
        int index = 0;
        while (null == resultMap.get("result")) {
            System.out.print("\r" + spinners[index++ % spinners.length]);
            Thread.sleep(100);
        }
    }

}