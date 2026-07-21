package agentlearning.policy;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径围栏：所有文件类工具必须先经过它。
 * 定位：LLM 输入的路径合法性检查，不是沙箱（不提供进程隔离）。
 */
public class PathGuard {

    private final Path rootPath;

    public PathGuard(String root) {
        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException("项目根路径不能为空");
        }
        this.rootPath = Paths.get(root).toAbsolutePath().normalize();
    }

    public Path getRootPath() { return rootPath; }

    /**
     * 校验路径是否在项目根之内，返回安全的绝对路径。越界则抛异常。
     */
    public Path resolveSafe(String input) {
        if (input == null || input.isBlank()) {
            throw new SecurityException("路径不能为空");
        }
        Path raw = Paths.get(input);
        // 绝对路径直接规范化；相对路径基于项目根解析
        Path resolved = raw.isAbsolute()
                ? raw.normalize()
                : rootPath.resolve(raw).normalize();

        // normalize() 会把 a/../b 里的 .. 消解掉，
        // 消解后如果不再以 rootPath 开头，说明越界了
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException(
                    "路径越界: " + input + " 不在项目根 " + rootPath + " 之内");
        }
        return resolved;
    }
}