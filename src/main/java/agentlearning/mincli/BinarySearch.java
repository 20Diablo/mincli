package agentlearning.mincli;

/**
 * 二分查找工具类（含边界条件处理）。
 *
 * 核心思路：
 *  - 左闭右开区间 [low, high) 进行搜索，规避 high 溢出与死循环问题。
 *  - 空数组、单元素、首尾命中、未命中等情况均正确处理。
 */
public final class BinarySearch {

    private BinarySearch() {
        // 工具类，禁止实例化
    }

    /**
     * 在已排序（升序）的整数数组中二分查找目标值。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 要查找的目标值
     * @return 目标值在数组中的下标；若不存在返回 -1
     */
    public static int binarySearch(int[] arr, int target) {
        // 边界处理：空数组直接返回 -1
        if (arr == null || arr.length == 0) {
            return -1;
        }

        int low = 0;
        int high = arr.length;   // 左闭右开 [low, high)

        while (low < high) {
            // 使用 (high - low) / 2 避免 low + high 溢出
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                return mid;              // 命中
            } else if (arr[mid] < target) {
                low = mid + 1;           // 目标在右半区
            } else {
                high = mid;              // 目标在左半区（开区间）
            }
        }

        return -1;   // 未找到
    }

    /**
     * 查找第一个等于 target 的下标（处理重复元素）。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 要查找的目标值
     * @return 第一个等于 target 的下标；若不存在返回 -1
     */
    public static int findFirst(int[] arr, int target) {
        if (arr == null || arr.length == 0) {
            return -1;
        }

        int low = 0;
        int high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;   // 相等时也向左收，找第一个
            }
        }

        // low 是第一个 >= target 的位置
        if (low < arr.length && arr[low] == target) {
            return low;
        }
        return -1;
    }

    /**
     * 查找最后一个等于 target 的下标（处理重复元素）。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 要查找的目标值
     * @return 最后一个等于 target 的下标；若不存在返回 -1
     */
    public static int findLast(int[] arr, int target) {
        if (arr == null || arr.length == 0) {
            return -1;
        }

        int low = 0;
        int high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                low = mid + 1;   // 相等时也向右收，找最后一个
            } else {
                high = mid;
            }
        }

        // low 是第一个 > target 的位置，low-1 是最后一个 <= target 的位置
        if (low - 1 >= 0 && arr[low - 1] == target) {
            return low - 1;
        }
        return -1;
    }

    /**
     * 查找第一个 >= target 的下标（下界，用于插入位置等场景）。
     * 若所有元素都小于 target，返回 arr.length。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 目标值
     * @return 第一个 >= target 的下标，范围为 [0, arr.length]
     */
    public static int lowerBound(int[] arr, int target) {
        if (arr == null || arr.length == 0) {
            return 0;
        }

        int low = 0;
        int high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    /**
     * 查找第一个 > target 的下标（上界）。
     * 若所有元素都 <= target，返回 arr.length。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 目标值
     * @return 第一个 > target 的下标，范围为 [0, arr.length]
     */
    public static int upperBound(int[] arr, int target) {
        if (arr == null || arr.length == 0) {
            return 0;
        }

        int low = 0;
        int high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    /**
     * 泛型版本：对已实现 Comparable 的对象数组做二分查找。
     *
     * @param arr    已按升序排序的数组（非 null）
     * @param target 要查找的目标值（非 null）
     * @param <T>    元素类型
     * @return 目标值下标；若不存在返回 -1
     */
    public static <T extends Comparable<? super T>> int binarySearch(T[] arr, T target) {
        if (arr == null || arr.length == 0 || target == null) {
            return -1;
        }

        int low = 0;
        int high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            int cmp = arr[mid].compareTo(target);
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return -1;
    }
}
