package io.wdsj.asw.bukkit.util;

import java.util.HashMap;
//TODO: Add AntiSpam feature(maybe)
public class EntropyUtils {

    private static final class Num {

        int i;

        private Num() {
            i = 1;
        }
    }

    /**
     * 平均每个字符的信息熵<br>
     * 即{@code entropy(str) / Math.log(str.length())}
     *
     * @param str
     *            字符串
     * @return 平均信息熵(当字符串为)
     * @see #averageEntropy
     */
    public static double averageEntropy(String str) {
        return averageEntropy(str, entropy(str));
    }

    /**
     * 平均每个字符的信息熵<br>
     * 即{@code entropy(str) / Math.log(str.length())}
     *
     * @param str
     *            字符串
     * @param entropy
     *            整体信息熵
     * @return 平均信息熵(当字符串为)
     */
    public static double averageEntropy(String str, double entropy) {
        if (str.length() <= 1) return Double.NaN;
        return entropy / Math.log(str.length());
    }

    /**
     * 信息熵
     *
     * @param str
     *            要计算的字符串
     * @return 信息熵
     */
    public static double entropy(String str) {
        if (str.length() <= 1) return 0.0D;
        if (str.length() == 2) {
            if (str.charAt(0) != str.charAt(1))
                return 1.0D;
            else
                return 0.0D;
        }
        double H = .0;
        str = str.toUpperCase(); // 将小写字母转换成大写
        HashMap<Character, Num> datas = new HashMap<>();
        for (int i = 0; i < str.length(); i++) { // 统计字母个数
            char c = str.charAt(i);
            Num num = datas.get(c);
            if (num == null)
                datas.put(c, new Num());
            else
                num.i++;
        }
        // 计算信息熵，将字母出现的频率作为离散概率值
        int len = str.length();
        double log = Math.log(2);
        for (Num num : datas.values()) {
            double p = 1.0 * num.i / len;// 单个字母的频率
            H -= p * (Math.log(p) / log);// H = -∑Pi*log2(Pi)
        }
        return H;
    }

    /**
     * 返回两条字符串的最短编辑距离,
     * 即将word2转变成word1的最小操作次数。
     * 采用二维动态规划实现，时间复杂度O(N^2)
     *
     * @param word1
     *            字符串1
     * @param word2
     *            字符串2
     * @return 最短编辑距离
     */
    public static int getMinDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        if (m == 0) return n;
        if (n == 0) return m;
        int[][] f = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++)
            f[i][0] = i;
        for (int j = 0; j <= n; j++)
            f[0][j] = j;

        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                if (word1.charAt(i - 1) == word2.charAt(j - 1))
                    f[i][j] = f[i - 1][j - 1];
                else
                    f[i][j] = min(f[i - 1][j - 1], f[i - 1][j], f[i][j - 1]) + 1;

        return f[m][n];
    }

    /**
     * 返回两个字符串的相似度。 当某个串长度小于5的时候，认为其不构成可比性
     *
     * @param word1
     *            字符串1
     * @param word2
     *            字符串2
     * @return float [0,100] 相似度
     * @see #getSimilarity(String, String, double)
     *
     */
    public static double getSimilarity(String word1, String word2) {
        double distance = getMinDistance(word1, word2);
        return 1 - distance / (Math.max(word1.length(), word2.length()));
    }

    /**
     * 返回两个字符串的相似度。 当某个串长度小于5的时候，认为其不构成可比性
     *
     * @param word1
     *            字符串1
     * @param word2
     *            字符串2
     * @param distance
     *            字符串距离
     * @return float [0,100] 相似度
     *
     */
    public static double getSimilarity(String word1, String word2, double distance) {
        return 1 - distance / (Math.max(word1.length(), word2.length()));
    }

    /**
     * 去三个数字中最小值
     *
     * @param a
     *            数字
     * @param b
     *            数字
     * @param c
     *            数字
     * @return 最小的数字
     */
    private static int min(int a, int b, int c) {
        return (a > b ? (Math.min(b, c)) : (Math.min(a, c)));
    }

    /**
     * 禁止实例化
     */
    private EntropyUtils() {
    }
}
