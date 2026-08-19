package agentlearning.mincli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 二分查找边界条件测试。
 */
class BinarySearchTest {

    // ==================== 正常情况 ====================

    @Test
    void targetInMiddle() {
        assertEquals(2, BinarySearch.binarySearch(new int[]{1, 2, 3, 4, 5, 6, 7}, 3));
    }

    @Test
    void targetAtStart() {
        assertEquals(0, BinarySearch.binarySearch(new int[]{1, 2, 3, 4, 5}, 1));
    }

    @Test
    void targetAtEnd() {
        assertEquals(4, BinarySearch.binarySearch(new int[]{1, 2, 3, 4, 5}, 5));
    }

    @Test
    void targetInLeftHalf() {
        assertEquals(1, BinarySearch.binarySearch(new int[]{1, 2, 3, 4, 5}, 2));
    }

    @Test
    void targetInRightHalf() {
        assertEquals(3, BinarySearch.binarySearch(new int[]{1, 2, 3, 4, 5}, 4));
    }

    @Test
    void singleElement_found() {
        assertEquals(0, BinarySearch.binarySearch(new int[]{7}, 7));
    }

    @Test
    void twoElements_foundFirst() {
        assertEquals(0, BinarySearch.binarySearch(new int[]{1, 9}, 1));
    }

    @Test
    void twoElements_foundSecond() {
        assertEquals(1, BinarySearch.binarySearch(new int[]{1, 9}, 9));
    }

    @Test
    void threeElements_foundMiddle() {
        assertEquals(1, BinarySearch.binarySearch(new int[]{1, 5, 9}, 5));
    }

    @Test
    void largerArray_trackedTarget() {
        // 递增大数组，验证算法在较大规模下的正确性
        int[] arr = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25};
        assertEquals(6, BinarySearch.binarySearch(arr, 13));
        assertEquals(0, BinarySearch.binarySearch(arr, 1));
        assertEquals(12, BinarySearch.binarySearch(arr, 25));
    }

    // ==================== 边界情况 ====================

    @Test
    void nullArray_returnsNegOne() {
        assertEquals(-1, BinarySearch.binarySearch((int[]) null, 5));
    }

    @Test
    void emptyArray_returnsNegOne() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{}, 5));
    }

    @Test
    void singleElement_notFound() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{7}, 3));
    }

    @Test
    void arrayWithNegativeNumbers_found() {
        assertEquals(0, BinarySearch.binarySearch(new int[]{-5, -3, -1, 0, 2, 4}, -5));
        assertEquals(3, BinarySearch.binarySearch(new int[]{-5, -3, -1, 0, 2, 4}, 0));
    }

    @Test
    void allNegativeNumbers_searchPositive() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{-10, -8, -6, -4}, 5));
    }

    @Test
    void largeArray_noOverflow() {
        // 验证 mid 计算不会溢出（元素值较大的场景）
        int[] arr = new int[]{Integer.MIN_VALUE, -1000, 0, 1000, Integer.MAX_VALUE};
        assertEquals(4, BinarySearch.binarySearch(arr, Integer.MAX_VALUE));
        assertEquals(0, BinarySearch.binarySearch(arr, Integer.MIN_VALUE));
    }

    @Test
    void allIdenticalElements_found() {
        // 所有元素相同，中间位置也应能找到
        assertEquals(2, BinarySearch.binarySearch(new int[]{5, 5, 5, 5, 5}, 5));
    }

    // ==================== 不存在元素 ====================

    @Test
    void targetSmallestBelowAll() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{10, 20, 30}, 5));
    }

    @Test
    void targetLargestAboveAll() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{10, 20, 30}, 40));
    }

    @Test
    void targetBetweenElements() {
        // 目标值位于两个相邻元素之间
        assertEquals(-1, BinarySearch.binarySearch(new int[]{1, 3, 5, 7, 9}, 4));
    }

    @Test
    void targetClosestToStart_notFound() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{10, 20, 30}, 9));
    }

    @Test
    void targetClosestToEnd_notFound() {
        assertEquals(-1, BinarySearch.binarySearch(new int[]{10, 20, 30}, 31));
    }

    @Test
    void targetMissingInLargerArray() {
        int[] arr = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
        assertEquals(-1, BinarySearch.binarySearch(arr, 11));
        assertEquals(-1, BinarySearch.binarySearch(arr, 1));
        assertEquals(-1, BinarySearch.binarySearch(arr, 21));
    }

    // ==================== findFirst / findLast 重复元素 ====================

    @Test
    void findFirst_withDuplicates() {
        // {1, 2, 2, 2, 3} 第一个 2 在下标 1
        assertEquals(1, BinarySearch.findFirst(new int[]{1, 2, 2, 2, 3}, 2));
        assertEquals(0, BinarySearch.findFirst(new int[]{5, 5, 5}, 5));
        assertEquals(-1, BinarySearch.findFirst(new int[]{1, 2, 4}, 3));
    }

    @Test
    void findLast_withDuplicates() {
        // {1, 2, 2, 2, 3, 3, 5} 最后一个 3 在下标 5
        assertEquals(5, BinarySearch.findLast(new int[]{1, 2, 2, 2, 3, 3, 5}, 3));
        assertEquals(2, BinarySearch.findLast(new int[]{5, 5, 5}, 5));
        assertEquals(-1, BinarySearch.findLast(new int[]{1, 2, 4}, 3));
    }

    @Test
    void findFirst_singleOccurrence() {
        assertEquals(0, BinarySearch.findFirst(new int[]{1, 2, 3}, 1));
        assertEquals(2, BinarySearch.findFirst(new int[]{1, 2, 3}, 3));
    }

    @Test
    void findLast_middleElement() {
        assertEquals(1, BinarySearch.findLast(new int[]{1, 2, 3}, 2));
    }

    @Test
    void findFirstLast_nullOrEmpty() {
        assertEquals(-1, BinarySearch.findFirst(null, 1));
        assertEquals(-1, BinarySearch.findFirst(new int[]{}, 1));
        assertEquals(-1, BinarySearch.findLast(null, 1));
        assertEquals(-1, BinarySearch.findLast(new int[]{}, 1));
    }

    // ==================== lowerBound / upperBound ====================

    @Test
    void lowerBound_Basic() {
        assertEquals(2, BinarySearch.lowerBound(new int[]{1, 3, 5, 7}, 5));
        assertEquals(2, BinarySearch.lowerBound(new int[]{1, 3, 5, 7}, 4));   // 5 是第一个 >=4
        assertEquals(4, BinarySearch.lowerBound(new int[]{1, 3, 5, 7}, 100)); // 全部小于
        assertEquals(0, BinarySearch.lowerBound(new int[]{1, 3, 5, 7}, 0));   // 全部大于
    }

    @Test
    void upperBound_Basic() {
        assertEquals(3, BinarySearch.upperBound(new int[]{1, 3, 5, 7}, 5));
        assertEquals(2, BinarySearch.upperBound(new int[]{1, 3, 5, 7}, 4));   // 5 是第一个 >4
        assertEquals(4, BinarySearch.upperBound(new int[]{1, 3, 5, 7}, 100)); // 全部 <=100
        assertEquals(0, BinarySearch.upperBound(new int[]{1, 3, 5, 7}, 0));   // 全部 >0
    }

    @Test
    void lowerUpperBound_withDuplicates() {
        int[] arr = {1, 2, 2, 2, 3, 3, 3, 5};
        assertEquals(1, BinarySearch.lowerBound(arr, 2)); // 第一个 >=2
        assertEquals(4, BinarySearch.upperBound(arr, 2)); // 第一个 >2
        assertEquals(4, BinarySearch.lowerBound(arr, 3)); // 第一个 >=3
        assertEquals(7, BinarySearch.upperBound(arr, 3)); // 第一个 >3
    }

    @Test
    void lowerUpperBound_nullOrEmpty() {
        assertEquals(0, BinarySearch.lowerBound(null, 1));
        assertEquals(0, BinarySearch.lowerBound(new int[]{}, 1));
        assertEquals(0, BinarySearch.upperBound(null, 1));
        assertEquals(0, BinarySearch.upperBound(new int[]{}, 1));
    }

    // ==================== 泛型版本 ====================

    @Test
    void generic_found() {
        String[] arr = {"apple", "banana", "cherry", "date"};
        assertEquals(1, BinarySearch.binarySearch(arr, "banana"));
    }

    @Test
    void generic_notFound() {
        Integer[] arr = {10, 20, 30, 40};
        assertEquals(-1, BinarySearch.binarySearch(arr, 25));
    }

    @Test
    void generic_boundaryHead() {
        String[] arr = {"apple", "banana", "cherry", "date"};
        assertEquals(0, BinarySearch.binarySearch(arr, "apple"));
    }

    @Test
    void generic_boundaryTail() {
        String[] arr = {"apple", "banana", "cherry", "date"};
        assertEquals(3, BinarySearch.binarySearch(arr, "date"));
    }

    @Test
    void generic_emptyOrNull() {
        assertEquals(-1, BinarySearch.binarySearch((String[]) null, "x"));
        assertEquals(-1, BinarySearch.binarySearch(new String[]{}, "x"));
        assertEquals(-1, BinarySearch.binarySearch(new String[]{"a"}, null));
    }

    @Test
    void generic_nullTarget() {
        Integer[] arr = {1, 2, 3};
        assertEquals(-1, BinarySearch.binarySearch(arr, null));
    }

    // ==================== 综合边界 ====================

    @Test
    void boundary_duplicatedTargetFirstAndLast() {
        int[] arr = {3, 3, 3, 3, 3};
        assertEquals(0, BinarySearch.findFirst(arr, 3));
        assertEquals(4, BinarySearch.findLast(arr, 3));
        assertEquals(0, BinarySearch.lowerBound(arr, 3));
        assertEquals(5, BinarySearch.upperBound(arr, 3));
        assertEquals(2, BinarySearch.binarySearch(arr, 3));
    }

    @Test
    void boundary_singleElementAllMethods() {
        int[] arr = {42};
        assertEquals(0, BinarySearch.binarySearch(arr, 42));
        assertEquals(-1, BinarySearch.binarySearch(arr, 0));
        assertEquals(0, BinarySearch.findFirst(arr, 42));
        assertEquals(0, BinarySearch.findLast(arr, 42));
        assertEquals(-1, BinarySearch.findFirst(arr, 0));
        assertEquals(-1, BinarySearch.findLast(arr, 0));
        assertEquals(0, BinarySearch.lowerBound(arr, 42));
        assertEquals(1, BinarySearch.upperBound(arr, 42));
    }
}
