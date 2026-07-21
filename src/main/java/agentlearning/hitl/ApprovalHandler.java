package agentlearning.hitl;

import java.util.Scanner;

/**
 * 人工审批：危险操作执行前，在终端问用户 y/n。
 */
public class ApprovalHandler {

    private final Scanner scanner;

    public ApprovalHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * 返回 true 表示用户批准执行。
     */
    public boolean confirm(String toolName, String detail) {
        System.out.println("\n⚠️  Agent 想执行一个操作，需要你确认：");
        System.out.println("    工具: " + toolName);
        System.out.println("    详情: " + detail);
        System.out.print("    是否允许？(y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }
}