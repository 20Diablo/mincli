package agentlearning.cli;

import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import okio.JvmSystemFileSystem;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class Main {

    public static void main(String[] args) throws Exception {
        // 从环境变量读 key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        DeepSeekClient client = new DeepSeekClient(apiKey);

        // 维护对话历史（多轮对话靠它）
        List<Message> history = new ArrayList<>();
        history.add(Message.system("你是一个简洁友好的助手。"));

        Scanner scanner = new Scanner(System.in);

        while(true){
            System.out.print("\n你: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;
            history.add(Message.user(input));           // 把用户输入加进历史
            String reply = client.chat(history);         // 发给模型
            history.add(Message.assistant(reply));        // 把回复也加进历史（下一轮要带上）
            System.out.println("助手: " + reply);
        }
        System.out.println("再见");
    }

}
