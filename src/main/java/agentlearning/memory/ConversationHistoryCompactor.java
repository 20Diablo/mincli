package agentlearning.memory;

import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConversationHistoryCompactor {

    private final DeepSeekClient client;
    private final int retainRecentRounds;   // 保留最近几个 user 轮次

    private static final String SUMMARY_PROMPT = """
            请把下面的对话历史压缩成简明摘要，保留：
            1. 用户提出的关键诉求与目标
            2. 已经完成的关键操作（调了什么工具、返回了什么核心结果）
            3. 已达成的共识或结论
            4. 仍未解决的问题或待办

            不要复述每条原文，不要列举所有工具调用，不要保留闲聊。
            输出 1-3 段中文，不要用列表，不要加任何前缀。

            === 待压缩的对话 ===
            %s
            === 结束 ===
            """;

    public ConversationHistoryCompactor(DeepSeekClient client) {
        this(client, 3);
    }
    public ConversationHistoryCompactor(DeepSeekClient client, int retainRecentRounds) {
        this.client = client;
        this.retainRecentRounds = Math.max(1, retainRecentRounds);
    }

    /**
     * 如果 token 超过 triggerTokens，就压缩 history（原地修改）。
     * @return 是否真的压缩了
     */
    public boolean compactIfNeeded(List<Message> history, int triggerTokens) {
        if (history == null || history.isEmpty()) return false;

        // 1. 估算当前 token，没超就不压
        int currentTokens = TokenBudget.estimateMessagesTokens(history);
        if (currentTokens < triggerTokens) return false;

        // 2. system 消息保留在最前面
        int systemEnd = (!history.isEmpty() && "system".equals(history.get(0).getRole())) ? 1 : 0;

        // 3. 找出所有 user 消息的下标
        List<Integer> userIndices = new ArrayList<>();
        for (int i = systemEnd; i < history.size(); i++) {
            if ("user".equals(history.get(i).getRole())) {
                userIndices.add(i);
            }
        }
        // user 轮次还不够多，不值得压
        if (userIndices.size() <= retainRecentRounds) return false;

        // 4. 分割点 = 倒数第 retainRecentRounds 个 user 的位置（保证落在 user 边界！）
        int splitIdx = userIndices.get(userIndices.size() - retainRecentRounds);
        if (splitIdx <= systemEnd) return false;

        // 5. 把 [systemEnd, splitIdx) 这段老消息拿去摘要
        List<Message> oldMsgs = new ArrayList<>(history.subList(systemEnd, splitIdx));
        if (oldMsgs.isEmpty()) return false;

        String summary;
        try {
            summary = summarize(oldMsgs);
        } catch (IOException e) {
            System.out.println("  [压缩失败，跳过] " + e.getMessage());
            return false;   // 摘要失败就别压，保持原样
        }
        if (summary == null || summary.isBlank()) return false;

        // 6. 重建 history: [system] + [摘要(伪装成user)] + [assistant确认] + [最近N轮]
        List<Message> rebuilt = new ArrayList<>();
        for (int i = 0; i < systemEnd; i++) rebuilt.add(history.get(i));
        rebuilt.add(Message.user("[已压缩的历史对话摘要]\n" + summary.trim()));
        rebuilt.add(Message.assistant("好的，我已了解之前的上下文，请继续。"));
        rebuilt.addAll(history.subList(splitIdx, history.size()));

        int afterTokens = TokenBudget.estimateMessagesTokens(rebuilt);
        history.clear();
        history.addAll(rebuilt);
        System.out.printf("  [已压缩历史] token %d -> %d，消息 %d 条%n",
                currentTokens, afterTokens, rebuilt.size());
        return true;
    }

    /** 调 LLM 生成摘要 */
    private String summarize(List<Message> messages) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            sb.append(m.getRole().toUpperCase()).append(": ");
            if (m.getContent() != null) sb.append(m.getContent());
            if (m.getToolCalls() != null) {
                m.getToolCalls().forEach(tc ->
                        sb.append("\n  [调用工具] ").append(tc.getFunction().getName())
                                .append(" ").append(tc.getFunction().getArguments()));
            }
            sb.append("\n\n");
        }
        String prompt = String.format(SUMMARY_PROMPT, sb);

        // 用一个临时的消息列表调 LLM（不带工具）
        List<Message> req = new ArrayList<>();
        req.add(Message.system("你是一个对话摘要助手，只输出摘要本身。"));
        req.add(Message.user(prompt));
        ChatResult result = client.chat(req, null);   // 第二个参数传 null = 不带工具
        return result.content;
    }
}