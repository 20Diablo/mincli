package agentlearning.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.function.Function;

//模型返回的"要调哪个工具"，用一个类接住
@JsonIgnoreProperties(ignoreUnknown = true)   // 忽略响应里我们没定义的字段
public class ToolCall {
    private String id;
    private String type;
    private Function function;

    public ToolCall(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Function{
        private String name;
        private String arguments;// 注意：这是 JSON 字符串，不是对象

        public  Function(){}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String augment) {
            this.arguments = augment;
        }
    }
}
