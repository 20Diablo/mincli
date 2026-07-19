package agentlearning.llm;

public class Message {
    private String role;
    private String content;

    public Message(){}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    // 几个方便的工厂方法，有哪些角色
    public static Message system(String content) { return new Message("system", content); }
    public static Message user(String content)   { return new Message("user", content); }
    public static Message assistant(String content) { return new Message("assistant", content); }
}
