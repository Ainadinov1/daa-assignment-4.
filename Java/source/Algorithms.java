import java.util.*;

/**
 * Implementation of Assignment 4 algorithms.
 * See docs/guide/CONTRACTS.md for specifications.
 */
public final class Algorithms {

    private Algorithms() {}

    /**
     * Part 1: LCS
     * Compare two versions of a sequence while preserving the order of shared characters.
     */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("length", 0L);
            res.put("sequence", "");
            return res;
        }
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                if (ca == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        int len = dp[n][m];
        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sb.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        String seq = sb.reverse().toString();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("length", (long) len);
        res.put("sequence", seq);
        return res;
    }

    /**
     * Part 2: Ordered adjacent-block merging - Interval DP
     */
    static Map<String, Object> merge(Map<String, Object> input) {
        @SuppressWarnings("unchecked")
        List<Object> rawSizes = (List<Object>) input.get("sizes");
        int n = rawSizes != null ? rawSizes.size() : 0;
        if (n <= 1) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("cost", 0L);
            res.put("plan", Collections.emptyList());
            return res;
        }
        long[] sizes = new long[n];
        for (int i = 0; i < n; i++) {
            sizes[i] = ((Number) rawSizes.get(i)).longValue();
        }
        long[] pref = new long[n + 1];
        for (int i = 0; i < n; i++) {
            pref[i + 1] = pref[i] + sizes[i];
        }
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++) {
            for (int l = 0; l <= n - len; l++) {
                int r = l + len - 1;
                long minCost = Long.MAX_VALUE;
                int bestK = l;
                long sum = pref[r + 1] - pref[l];
                for (int k = l; k < r; k++) {
                    long c = dp[l][k] + dp[k + 1][r] + sum;
                    if (c < minCost) {
                        minCost = c;
                        bestK = k;
                    }
                }
                dp[l][r] = minCost;
                split[l][r] = bestK;
            }
        }
        List<List<Long>> plan = new ArrayList<>();
        buildMergePlan(0, n - 1, split, plan);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("cost", dp[0][n - 1]);
        res.put("plan", plan);
        return res;
    }

    private static void buildMergePlan(int l, int r, int[][] split, List<List<Long>> plan) {
        if (l >= r) return;
        int k = split[l][r];
        buildMergePlan(l, k, split, plan);
        buildMergePlan(k + 1, r, split, plan);
        plan.add(List.of((long) l, (long) k, (long) r));
    }

    /**
     * Part 3: Maximum weight independent set on a tree - Tree DP
     */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        if (n == 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("weight", 0L);
            res.put("selected", Collections.emptyList());
            return res;
        }
        @SuppressWarnings("unchecked")
        List<Object> rawWeights = (List<Object>) input.get("weights");
        long[] weights = new long[n];
        for (int i = 0; i < n; i++) {
            weights[i] = ((Number) rawWeights.get(i)).longValue();
        }
        @SuppressWarnings("unchecked")
        List<Object> rawEdges = (List<Object>) input.get("edges");
        int m = (n > 1 && rawEdges != null) ? rawEdges.size() : 0;
        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] to = new int[2 * m];
        int[] nextEdge = new int[2 * m];
        int edgeCount = 0;
        if (rawEdges != null) {
            for (Object eo : rawEdges) {
                @SuppressWarnings("unchecked")
                List<Object> edge = (List<Object>) eo;
                int u = ((Number) edge.get(0)).intValue();
                int v = ((Number) edge.get(1)).intValue();
                to[edgeCount] = v;
                nextEdge[edgeCount] = head[u];
                head[u] = edgeCount++;
                to[edgeCount] = u;
                nextEdge[edgeCount] = head[v];
                head[v] = edgeCount++;
            }
        }

        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        int[] bfsOrder = new int[n];
        int headIdx = 0, tailIdx = 0;
        bfsOrder[tailIdx++] = 0;
        parent[0] = -2;
        while (headIdx < tailIdx) {
            int u = bfsOrder[headIdx++];
            for (int e = head[u]; e != -1; e = nextEdge[e]) {
                int v = to[e];
                if (parent[v] == -1) {
                    parent[v] = u;
                    bfsOrder[tailIdx++] = v;
                }
            }
        }
        parent[0] = -1;

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int idx = n - 1; idx >= 0; idx--) {
            int u = bfsOrder[idx];
            long sumSkip = 0;
            long sumMax = 0;
            for (int e = head[u]; e != -1; e = nextEdge[e]) {
                int v = to[e];
                if (v != parent[u]) {
                    sumSkip += skip[v];
                    sumMax += Math.max(take[v], skip[v]);
                }
            }
            take[u] = weights[u] + sumSkip;
            skip[u] = sumMax;
        }

        boolean[] isTaken = new boolean[n];
        if (take[0] > skip[0]) {
            isTaken[0] = true;
        } else {
            isTaken[0] = false;
        }

        List<Long> selected = new ArrayList<>();
        if (isTaken[0]) {
            selected.add(0L);
        }
        for (int idx = 1; idx < n; idx++) {
            int u = bfsOrder[idx];
            int p = parent[u];
            if (isTaken[p]) {
                isTaken[u] = false;
            } else {
                if (take[u] > skip[u]) {
                    isTaken[u] = true;
                    selected.add((long) u);
                } else {
                    isTaken[u] = false;
                }
            }
        }

        long maxWeight = Math.max(take[0], skip[0]);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("weight", maxWeight);
        res.put("selected", selected);
        return res;
    }

    /**
     * Part 4: Naive exact string search
     */
    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        List<Long> positions = new ArrayList<>();
        int n = text != null ? text.length() : 0;
        int m = pattern != null ? pattern.length() : 0;
        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
            return Map.of("positions", positions);
        }
        if (m > n) {
            return Map.of("positions", positions);
        }
        for (int i = 0; i <= n - m; i++) {
            boolean match = true;
            for (int j = 0; j < m; j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                positions.add((long) i);
            }
        }
        return Map.of("positions", positions);
    }

    /**
     * Part 4: KMP exact string search
     */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        List<Long> positions = new ArrayList<>();
        int n = text != null ? text.length() : 0;
        int m = pattern != null ? pattern.length() : 0;
        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
            return Map.of("positions", positions);
        }
        if (m > n) {
            return Map.of("positions", positions);
        }
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int j = pi[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = pi[j - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            pi[i] = j;
        }
        int j = 0;
        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = pi[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == m) {
                positions.add((long) (i - m + 1));
                j = pi[j - 1];
            }
        }
        return Map.of("positions", positions);
    }

    /**
     * Part 4: Rabin-Karp exact string search
     */
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        List<Long> positions = new ArrayList<>();
        int n = text != null ? text.length() : 0;
        int m = pattern != null ? pattern.length() : 0;
        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
            return Map.of("positions", positions);
        }
        if (m > n) {
            return Map.of("positions", positions);
        }
        final int BASE = 31;
        final int MOD = 101;
        int patternHash = 0;
        int windowHash = 0;
        int highPower = 1;
        for (int i = 0; i < m - 1; i++) {
            highPower = (highPower * BASE) % MOD;
        }
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + (int) pattern.charAt(i)) % MOD;
            windowHash = (windowHash * BASE + (int) text.charAt(i)) % MOD;
        }

        if (windowHash == patternHash) {
            boolean match = true;
            for (int j = 0; j < m; j++) {
                if (text.charAt(j) != pattern.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                positions.add(0L);
            }
        }

        for (int i = 1; i <= n - m; i++) {
            int charOut = (int) text.charAt(i - 1);
            int charIn = (int) text.charAt(i + m - 1);
            long temp = windowHash - (long) charOut * highPower;
            temp = (temp % MOD + MOD) % MOD;
            windowHash = (int) ((temp * BASE + charIn) % MOD);

            if (windowHash == patternHash) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    positions.add((long) i);
                }
            }
        }
        return Map.of("positions", positions);
    }

    /**
     * Part 5: Suffix array and Kasai LCP
     */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text != null ? text.length() : 0;
        if (n == 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("sa", Collections.emptyList());
            res.put("lcp", Collections.emptyList());
            res.put("repeated", "");
            return res;
        }
        if (n == 1) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("sa", List.of(0L));
            res.put("lcp", List.of(0L));
            res.put("repeated", "");
            return res;
        }

        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = (int) text.charAt(i);
        }

        long[] pairs = new long[n];
        for (int len = 1; len < n; len *= 2) {
            for (int i = 0; i < n; i++) {
                long r1 = rank[i] + 1;
                long r2 = (i + len < n) ? (rank[i + len] + 1) : 0;
                pairs[i] = (r1 << 36) | (r2 << 18) | i;
            }
            Arrays.sort(pairs);
            int currentRank = 0;
            rank[(int) (pairs[0] & 0x3FFFF)] = 0;
            for (int i = 1; i < n; i++) {
                if ((pairs[i] >> 18) != (pairs[i - 1] >> 18)) {
                    currentRank++;
                }
                rank[(int) (pairs[i] & 0x3FFFF)] = currentRank;
            }
            if (currentRank == n - 1) {
                break;
            }
        }

        int[] sa = new int[n];
        for (int i = 0; i < n; i++) {
            sa[rank[i]] = i;
        }

        int[] lcp = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] == 0) {
                k = 0;
                continue;
            }
            int j = sa[rank[i] - 1];
            while (i + k < n && j + k < n && text.charAt(i + k) == text.charAt(j + k)) {
                k++;
            }
            lcp[rank[i]] = k;
            if (k > 0) {
                k--;
            }
        }
        lcp[0] = 0;

        int maxLcp = 0;
        int maxIdx = -1;
        for (int i = 1; i < n; i++) {
            if (lcp[i] > maxLcp) {
                maxLcp = lcp[i];
                maxIdx = i;
            }
        }
        String repeated = (maxLcp > 0) ? text.substring(sa[maxIdx], sa[maxIdx] + maxLcp) : "";

        List<Long> saList = new ArrayList<>(n);
        List<Long> lcpList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            saList.add((long) sa[i]);
            lcpList.add((long) lcp[i]);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("sa", saList);
        res.put("lcp", lcpList);
        res.put("repeated", repeated);
        return res;
    }

    /**
     * Part 6: Aho-Corasick dictionary search
     */
    static Map<String, Object> aho(Map<String, Object> input) {
        @SuppressWarnings("unchecked")
        List<Object> rawPatterns = (List<Object>) input.get("patterns");
        @SuppressWarnings("unchecked")
        List<Object> rawTexts = (List<Object>) input.get("texts");

        int numPatterns = rawPatterns != null ? rawPatterns.size() : 0;
        int numTexts = rawTexts != null ? rawTexts.size() : 0;

        if (numTexts == 0) {
            return Map.of("counts", Collections.emptyList());
        }

        if (numPatterns == 0) {
            List<List<Long>> counts = new ArrayList<>(numTexts);
            for (int i = 0; i < numTexts; i++) {
                counts.add(Collections.emptyList());
            }
            return Map.of("counts", counts);
        }

        String[] patterns = new String[numPatterns];
        int totalPatternLen = 0;
        for (int i = 0; i < numPatterns; i++) {
            patterns[i] = (String) rawPatterns.get(i);
            totalPatternLen += patterns[i].length();
        }

        int maxStates = totalPatternLen + 1;
        final int ALPHABET = 95;
        int[][] next = new int[maxStates][ALPHABET];
        int[] fail = new int[maxStates];
        int numStates = 1;

        int[] endState = new int[numPatterns];

        for (int p = 0; p < numPatterns; p++) {
            String pat = patterns[p];
            int u = 0;
            for (int i = 0; i < pat.length(); i++) {
                int c = pat.charAt(i) - 32;
                if (c >= 0 && c < ALPHABET) {
                    if (next[u][c] == 0) {
                        next[u][c] = numStates++;
                    }
                    u = next[u][c];
                }
            }
            endState[p] = u;
        }

        int[] queue = new int[numStates];
        int qHead = 0, qTail = 0;
        int[] bfsOrder = new int[numStates];
        int bfsCount = 0;

        for (int c = 0; c < ALPHABET; c++) {
            int v = next[0][c];
            if (v != 0) {
                fail[v] = 0;
                queue[qTail++] = v;
            }
        }

        while (qHead < qTail) {
            int u = queue[qHead++];
            bfsOrder[bfsCount++] = u;
            for (int c = 0; c < ALPHABET; c++) {
                int v = next[u][c];
                if (v != 0) {
                    fail[v] = next[fail[u]][c];
                    queue[qTail++] = v;
                } else {
                    next[u][c] = next[fail[u]][c];
                }
            }
        }

        List<List<Long>> allCounts = new ArrayList<>(numTexts);
        long[] visits = new long[numStates];

        for (int t = 0; t < numTexts; t++) {
            String txt = (String) rawTexts.get(t);
            Arrays.fill(visits, 0);

            if (txt != null && !txt.isEmpty()) {
                int u = 0;
                for (int i = 0; i < txt.length(); i++) {
                    int c = txt.charAt(i) - 32;
                    if (c >= 0 && c < ALPHABET) {
                        u = next[u][c];
                    } else {
                        u = 0;
                    }
                    visits[u]++;
                }

                for (int i = bfsCount - 1; i >= 0; i--) {
                    int v = bfsOrder[i];
                    visits[fail[v]] += visits[v];
                }
            }

            List<Long> row = new ArrayList<>(numPatterns);
            for (int p = 0; p < numPatterns; p++) {
                row.add(visits[endState[p]]);
            }
            allCounts.add(row);
        }

        return Map.of("counts", allCounts);
    }
}
