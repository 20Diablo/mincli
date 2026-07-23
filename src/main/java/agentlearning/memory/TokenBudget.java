package agentlearning.memory;

import agentlearning.llm.Message;
import agentlearning.llm.ToolCall;

import java.util.List;

/**
 * Token 预算：粗略估算消息列表的 token 数。
 * 不追求精确（精确要用 tokenizer），够用来判断"是否该压缩"即可。
 */
public class TokenBudget {

    // 经验值：中英文混合大约 2 个字符 ≈ 1 token。偏保守（宁可高估）。
    private static final double CHARS_PER_TOKEN = 2.0;

    /** 估算单条消息的 token */
    public static int estimateMessageTokens(Message m) {
        int chars = 0;
        if (m.getRole() != null) chars += m.getRole().length();
        if (m.getContent() != null) chars += m.getContent().length();
        if (m.getToolCalls() != null) {
            for (ToolCall tc : m.getToolCalls()) {
                if (tc.getFunction() != null) {
                    chars += tc.getFunction().getName().length();
                    if (tc.getFunction().getArguments() != null) {
                        chars += tc.getFunction().getArguments().length();
                    }
                }
            }
        }
        if (m.getToolCallId() != null) chars += m.getToolCallId().length();
        // 每条消息有固定结构开销（role 包装等），加个常数
        return (int) (chars / CHARS_PER_TOKEN) + 4;
    }

    /** 估算整个消息列表的 token */
    public static int estimateMessagesTokens(List<Message> messages) {
        int total = 0;
        for (Message m : messages) {
            total += estimateMessageTokens(m);
        }
        return total;
    }
}