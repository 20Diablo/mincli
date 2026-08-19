package agentlearning.rag;

public record CodeChunk(String filePath, String name, String content, int startLine, int endLine) {
    @Override
    public String toString() {
        return filePath + ":" + startLine + "-" + endLine + " [" + name + "]";
    }
}