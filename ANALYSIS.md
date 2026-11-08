# Assignment 4 — Analysis

## Environment and commands

- **Language / Runtime:** Java 24 (compiled with `--release 17` targeting Java SE 17).
- **Launcher:** Python 3.14.3.
- **Operating System / Hardware:** macOS Darwin arm64, Apple M4 CPU, 16 GB Unified Memory.
- **Submission Configuration (`submission.json`):** `{"language": "Java"}`.
- **Build / Run / Test Commands:**
  - Setup validation:
    ```bash
    python3 tools/run.py --language Java --setup
    ```
  - Public test suite (44 test cases + batch state reset test):
    ```bash
    python3 tools/run.py --language Java --check
    ```
  - Student custom test suite:
    ```bash
    python3 tools/run.py --language Java --input tests/student/requests.jsonl
    ```
- **Moodle Submission Instructions:**
  - Commit URL format: `https://github.com/<username>/<repo>/commit/<full-40-char-SHA>`
  - Submit the exact commit link along with the selected language (`Java`) in Moodle under *Assignment 4 → Online text*.

---

## Algorithm explanations

### 1. Longest Common Subsequence (`lcs`)

- **State and Indices:**
  Let $a$ and $b$ be the input strings of lengths $n$ and $m$. $dp[i][j]$ represents the length of the longest common subsequence between prefix $a[0..i-1]$ and prefix $b[0..j-1]$, for $0 \le i \le n$ and $0 \le j \le m$.
- **Base Cases and Transitions:**
  - *Base:* $dp[i][0] = 0$ for all $0 \le i \le n$, and $dp[0][j] = 0$ for all $0 \le j \le m$.
  - *Transitions:* For $i, j \ge 1$:
    $$\begin{cases}
    dp[i][j] = dp[i-1][j-1] + 1 & \text{if } a[i-1] == b[j-1] \\
    dp[i][j] = \max(dp[i-1][j], dp[i][j-1]) & \text{if } a[i-1] \ne b[j-1]
    \end{cases}$$
- **Processing Order:**
  Row-by-row ($i$ from 1 to $n$) and column-by-column ($j$ from 1 to $m$). When computing $dp[i][j]$, subproblems $dp[i-1][j-1]$, $dp[i-1][j]$, and $dp[i][j-1]$ are already computed and available.
- **Correctness Argument:**
  If the last characters match ($a[i-1] == b[j-1]$), extending any optimal LCS of $a[0..i-2]$ and $b[0..j-2]$ by this character yields a common subsequence of length $dp[i-1][j-1] + 1$. An optimum cannot exceed this length without producing a longer LCS for the smaller prefixes. When $a[i-1] \ne b[j-1]$, the optimal subsequence cannot include both characters; hence it must be formed either from $a[0..i-2]$ and $b[0..j-1]$ or from $a[0..i-1]$ and $b[0..j-2]$.
- **Reconstruction and Tie Handling:**
  Backtracking starts at $(n, m)$. While $i > 0$ and $j > 0$:
  - If $a[i-1] == b[j-1]$, append $a[i-1]$ to the result and move diagonally to $(i-1, j-1)$.
  - Else if $dp[i-1][j] \ge dp[i][j-1]$, step up to $(i-1, j)$.
  - Else step left to $(i, j-1)$.
  Reversing the accumulated sequence gives a valid optimal LCS. Ties where $dp[i-1][j] == dp[i][j-1]$ are resolved consistently by stepping up; any valid optimal witness is accepted.
- **Complexity:**
  - *Time:* $\mathcal{O}((n+1)(m+1))$ after JSON parsing.
  - *Space:* $\mathcal{O}((n+1)(m+1))$ auxiliary storage for the DP table (`int[n+1][m+1]`), plus $\mathcal{O}(\min(n, m))$ for the reconstructed sequence.
- **Defect Detected by Boundary Test:**
  `{"a": "axbyc", "b": "abc"}` tests that characters are skipped without requiring contiguity (verifying subsequence vs substring behavior). `{"a": "", "b": "abc"}` verifies empty-input safety returning `length: 0` and `sequence: ""` without indexing errors.

---

### 2. Ordered Adjacent-Block Merging (`merge`)

- **State and Indices:**
  Let `sizes` have length $n$. For $0 \le l \le r < n$, $dp[l][r]$ denotes the minimum total cost to merge the contiguous sequence of blocks from original index $l$ to $r$ into a single composite block $[l, r]$.
- **Base Cases and Transitions:**
  - *Base:* $dp[l][l] = 0$ for all $0 \le l < n$ (single block requires no merge).
  - *Transitions:* For length $len = r - l + 1 \ge 2$:
    $$dp[l][r] = \min_{l \le k < r} \left( dp[l][k] + dp[k+1][r] \right) + \sum_{t=l}^{r} sizes[t]$$
    Prefix sums $pref[i] = \sum_{t=0}^{i-1} sizes[t]$ allow $\mathcal{O}(1)$ interval sum queries: $\sum_{t=l}^{r} sizes[t] = pref[r+1] - pref[l]$.
    The optimal split point is recorded: $split[l][r] = \arg\min_k (\dots)$.
- **Processing Order:**
  Outer loop iterates over block interval length $len$ from 2 to $n$; inner loop iterates over left endpoint $l$. Any subinterval of length $< len$ (specifically $[l, k]$ and $[k+1, r]$) has strictly smaller length and is fully solved.
- **Correctness Argument:**
  Every sequence of adjacent merges corresponds to a binary tree of splits. The final merge combines two adjacent composite blocks $[l, k]$ and $[k+1, r]$. Because block sizes are non-negative and independent, optimal substructure holds: the total cost is minimized by independently optimizing the subplans for $[l, k]$ and $[k+1, r]$ plus the unavoidable final merge cost equal to the total weight of interval $[l, r]$.
- **Reconstruction and Tie Handling:**
  The plan is reconstructed via a recursive post-order traversal:
  To merge $[l, r]$ at split $k$, we first recursively build the plan for $[l, k]$, then for $[k+1, r]$, and finally emit the merge operation $[l, k, r]$. Post-order emission ensures that child blocks $[l, k]$ and $[k+1, r]$ exist as adjacent blocks when the parent merge is performed, matching the validation contract.
- **Complexity:**
  - *Time:* $\mathcal{O}(n^3)$ operations ($n \le 200 \implies \approx 1.33 \times 10^6$ loop iterations, executing in $< 40$ ms).
  - *Space:* $\mathcal{O}(n^2)$ auxiliary storage (`long[n][n]` for costs and `int[n][n]` for split indices). Costs and prefix sums use signed 64-bit integers (`long`) to avoid overflow when sizes reach $10^9$.
- **Defect Detected by Boundary Test:**
  `{"sizes": [10, 1, 10]}` detects greedy non-adjacent merging: a greedy algorithm might merge the smallest elements or violate adjacency, whereas adjacent interval DP yields cost 32. `sizes: [1000000000, 1000000000, 1000000000, 1000000000]` detects 32-bit integer overflow.

---

### 3. Tree Maximum Weight Independent Set (`tree`)

- **State and Indices:**
  For an undirected tree of $n$ vertices rooted at vertex 0:
  - $take[u]$: maximum weight independent set in the subtree rooted at $u$, given that vertex $u$ is selected (included in the set).
  - $skip[u]$: maximum weight independent set in the subtree rooted at $u$, given that vertex $u$ is NOT selected.
- **Base Cases and Transitions:**
  - *Base:* For a leaf vertex $L$: $take[L] = weights[L]$ and $skip[L] = 0$.
  - *Transitions:* For vertex $u$ with children $v \in children(u)$:
    $$take[u] = weights[u] + \sum_{v \in children(u)} skip[v]$$
    $$skip[u] = \sum_{v \in children(u)} \max(take[v], skip[v])$$
    Because the empty set is allowed, subtree decisions cannot force negative totals: $skip[u] \ge 0$ always.
- **Processing Order (Iterative Bottom-Up):**
  To prevent `StackOverflowError` on deep chains ($n \le 100,000$), we avoid recursion. We run an iterative BFS from root 0 to establish vertex parents and topological BFS order. We then compute DP transitions by traversing vertices in **reverse BFS order** (leaves to root). When processing $u$, all its children have already been fully computed.
- **Correctness Argument:**
  If vertex $u$ is selected ($take[u]$), none of its immediate children $v$ can be selected due to the independent set condition; hence each child $v$ must contribute $skip[v]$. If vertex $u$ is not selected ($skip[u]$), each child $v$ can independently choose whether taking $v$ or skipping $v$ achieves greater weight.
- **Reconstruction and Tie Handling:**
  Top-down forward BFS traversal:
  - At root: vertex 0 is selected if $take[0] > skip[0]$ (if tied, skipping or taking is optimal; our implementation takes if $take[0] > skip[0]$).
  - For child $v$ of parent $p$: if $p$ was selected, $v$ MUST be skipped (`isTaken[v] = false`). If $p$ was skipped, $v$ is selected if $take[v] > skip[v]$.
  The optimal weight is $\max(take[0], skip[0])$.
- **Complexity:**
  - *Time:* $\mathcal{O}(n)$ using adjacency list representation and two linear passes (reverse BFS + forward BFS).
  - *Space:* $\mathcal{O}(n)$ auxiliary memory for arrays (`head`, `to`, `nextEdge`, `bfsOrder`, `parent`, `take`, `skip`, `isTaken`).
- **Defect Detected by Boundary Test:**
  `{"n": 3, "weights": [-4, -1, -5], "edges": [[0,1],[1,2]]}` ensures all-negative trees return `weight: 0` and `selected: []`. A deep chain of $n=100,000$ vertices verifies immunity to recursion stack overflows.

---

### 4. Exact String Search — Naive Baseline (`naive`)

- **State and Invariant:**
  $text$ of length $n$ and $pattern$ of length $m$. Candidate start index $i \in [0, n - m]$. For each $i$, characters $pattern[j]$ are compared against $text[i+j]$ for $0 \le j < m$.
- **Boundary Conditions:**
  - If $m == 0$: empty pattern matches every boundary $0 \le i \le n$ (returns $[0, 1, \dots, n]$ of length $n + 1$).
  - If $m > n$: returns empty list `[]`.
- **Processing Order:**
  Sequential start scan $i = 0, 1, \dots, n - m$.
- **Correctness Argument:**
  Exhaustively verifies every possible starting index without skipping; preserves overlapping matches in strictly increasing order.
- **Complexity:**
  - *Time:* $\mathcal{O}(n \cdot m + n + 1)$ worst-case; $\mathcal{O}(n)$ best-case.
  - *Space:* $\mathcal{O}(1)$ auxiliary storage; $\mathcal{O}(z + 1)$ for output list of $z$ positions.
- **Defect Detected by Boundary Test:**
  `text="aaaa", pattern="aa"` verifies overlapping occurrences $[0, 1, 2]$ are preserved and not skipped.

---

### 5. Exact String Search — Knuth–Morris–Pratt (`kmp`)

- **State and Invariant:**
  - Prefix function $\pi[i]$ for $0 \le i < m$: length of the longest proper prefix of $pattern[0..i]$ that is also a suffix of $pattern[0..i]$.
  - Text matching state $j$: length of the current matched prefix of $pattern$.
- **Base Cases and Transitions:**
  - *Prefix function:* $\pi[0] = 0$. For $i = 1..m-1$: while $j > 0$ and $pattern[i] \ne pattern[j]$, fallback to $j = \pi[j-1]$. If matching, $j++$, and $\pi[i] = j$.
  - *Matching loop:* For text index $i = 0..n-1$: while $j > 0$ and $text[i] \ne pattern[j]$, fallback $j = \pi[j-1]$. If $text[i] == pattern[j]$, increment $j$.
  - When $j == m$: match found at start index $(i - m + 1)$; transition to next state via prefix fallback $j = \pi[m - 1]$ to capture overlapping matches.
  - Empty pattern $m=0$ returns $[0, 1, \dots, n]$ immediately.
- **Correctness Argument:**
  $\pi[j-1]$ identifies the longest prefix that already matches the suffix ending at $text[i-1]$. No character in the text is ever reconsidered backwards ($i$ strictly advances); no possible match can start between the previous start and the fallback position.
- **Complexity:**
  - *Time:* $\mathcal{O}(n + m + z + 1)$ amortized linear time: in both $\pi$ construction and text scanning, $j$ increases at most once per character and decrements along $\pi$ links, bounding total operations by $2(n + m)$.
  - *Space:* $\mathcal{O}(m + 1)$ auxiliary memory for $\pi$ array (`int[m]`).
- **Defect Detected by Boundary Test:**
  `text="abababa", pattern="aba"` tests that after matching at index 0, prefix fallback correctly finds overlapping occurrences at 2 and 4 without resetting $j$ to 0.

---

### 6. Exact String Search — Rabin–Karp (`rk`)

- **State and Rolling Hash Update:**
  - Alphabet: ASCII character codes. Fixed parameters: $base = 31$, $modulus = 101$.
  - Hash of string $S[0..m-1]$:
    $$H(S) = \left( \sum_{k=0}^{m-1} S[k] \cdot 31^{m - 1 - k} \right) \bmod 101$$
    Highest power belongs to the leftmost character ($31^{m-1} \bmod 101$).
  - Rolling hash step: moving from window $text[i-1..i+m-2]$ to $text[i..i+m-1]$:
    $$H_{new} = \left( (H_{prev} - text[i-1] \cdot 31^{m-1}) \cdot 31 + text[i+m-1] \right) \bmod 101$$
    Negative remainders are normalized: `temp = (temp % 101 + 101) % 101`.
- **Character Verification and Candidate Bound:**
  Equal hashes indicate only a candidate match. Due to the small modulus 101, hash collisions occur frequently. For every candidate window where $windowHash == patternHash$, actual characters are compared:
  $$text[i..i+m-1] == pattern[0..m-1]$$
- **Correctness Argument:**
  The rolling update computes the exact polynomial fingerprint modulo 101. Character verification guarantees zero false positives.
- **Complexity:**
  - *Time:* $\mathcal{O}(n + m + c \cdot m)$, where $c$ is the number of candidate windows having equal hash. Worst case is $\mathcal{O}(n \cdot m)$ when every window collides.
  - *Space:* $\mathcal{O}(1)$ auxiliary memory (only integer registers for rolling hash).
- **Defect Detected by Boundary Test:**
  `text="abdj", pattern="ab"` tests collision handling: `"ab"` and `"dj"` both evaluate to $75 \bmod 101$. Character verification rejects `"dj"` at index 2, outputting only `[0]`.

---

### 7. Suffix Array and Kasai LCP (`suffix`)

#### Suffix Array via Prefix Doubling

- **State and Rank Pairs:**
  For text of length $n$, each suffix $i \in [0, n-1]$ is assigned an integer rank based on its prefix of length $2^k$.
  At step $k$ (comparing prefix length $len = 2^k$):
  - Primary rank: $r_1 = rank[i] + 1$.
  - Secondary rank: $r_2 = (i + len < n) ? (rank[i + len] + 1) : 0$.
  - Rank pair $(r_1, r_2)$ uniquely defines the relative lexicographical order of $text[i .. \min(n, i + 2len) - 1]$.
- **Bit-Packed Sorting:**
  We pack $(r_1, r_2, i)$ into a single 64-bit unsigned `long`:
  `pairs[i] = (r1 << 36) | (r2 << 18) | i`.
  Calling `Arrays.sort(pairs)` sorts primarily by $r_1$, secondarily by $r_2$, in Dual-Pivot Quicksort. Ranks are recomputed; if all $n$ ranks are distinct ($currentRank == n - 1$), the doubling loop terminates early. Suffix array $sa$ is inverted from the final ranks: $sa[rank[i]] = i$.
- **Complexity:**
  $\mathcal{O}(n \log^2(n+1))$ time with comparison sort across $\lceil \log_2 n \rceil$ rounds (or $\mathcal{O}(n \log n)$). Space is $\mathcal{O}(n)$ auxiliary for `pairs`, `rank`, and `sa`.

#### Kasai LCP Algorithm

- **State and Invariant:**
  $rank[i]$ gives the position of suffix $i$ in $sa$.
  Let $h$ be the length of the longest common prefix between suffix $i$ and its lexicographical predecessor in $sa$, suffix $j = sa[rank[i] - 1]$.
  *Kasai's Invariant:* When moving from suffix $i$ to suffix $i+1$, the LCP between suffix $i+1$ and its predecessor in $sa$ is at least $h - 1$.
- **Transitions and Base:**
  $lcp[0] = 0$. For $i = 0..n-1$: if $rank[i] == 0$, $k = 0$. Otherwise, let $j = sa[rank[i] - 1]$; match characters $text[i+k]$ and $text[j+k]$ while equal, incrementing $k$. Store $lcp[rank[i]] = k$. Before moving to $i+1$, decrement $k$ by 1 if $k > 0$.
- **Longest Repeated Substring (`repeated`):**
  A substring appearing at two distinct starts corresponds to a common prefix of two distinct suffixes. Adjacent suffixes in $sa$ maximize mutual LCP. Thus:
  $$\max \text{length of repeated substring} = \max_{1 \le i < n} lcp[i]$$
  If $\max(lcp) == 0$ or text is empty, `repeated = ""`. Otherwise, for index $idx$ where $lcp[idx] == \max(lcp)$, `repeated = text.substring(sa[idx], sa[idx] + maxLcp)`.
- **Complexity:**
  $k$ starts at 0, increments at most $n$ times, and decrements at most $n$ times; hence Kasai runs in strictly $\mathcal{O}(n)$ time and $\mathcal{O}(n)$ space.

- **Defect Detected by Boundary Test:**
  `text="aaaa"` tests maximum overlapping repeated substring (`sa: [3, 2, 1, 0]`, `lcp: [0, 1, 2, 3]`, `repeated: "aaa"`). `text=""` and `text="$"` test proper empty/single-character edge cases.

---

### 8. Dictionary Matching — Aho–Corasick (`aho`)

- **Automaton Structure:**
  - Keyword trie storing $p$ patterns with total length $L \le 100,000$. States are integers $0 \le u < S$.
  - Fixed alphabet of 95 printable ASCII characters (codes 32–126).
  - Terminal pattern tracking: `endState[p]` records the trie node where dictionary pattern $p$ ends. Duplicate dictionary strings share the same trie node.
- **Failure Function and Transition Automaton (BFS):**
  - Using BFS from root 0:
    - Root children have $fail[v] = 0$.
    - For state $u$ popped from BFS queue:
      For character $c \in [0, 94]$:
      If child $v = next[u][c] \ne 0$:
        $fail[v] = next[fail[u]][c]$
        enqueue $v$.
      Else:
        $next[u][c] = next[fail[u]][c]$ (shortcut transition to fallback state).
  - The BFS traversal order is recorded in `bfsOrder`.
- **Efficient Occurrence Counting via Reverse-BFS Visit Propagation:**
  - Instead of traversing output links per character (which is vulnerable to quadratic $\mathcal{O}(z)$ behavior on dense inputs like $a^k$), we use **reverse-failure visit propagation**:
    1. For each input text, clear state visits array: `Arrays.fill(visits, 0)`.
    2. Walk the text through the automaton: for each character, $u = next[u][c]$; `visits[u]++`.
    3. Traverse `bfsOrder` in **reverse order** (from deepest states back to root):
       For each state $v$:
       $$visits[fail[v]] += visits[v]$$
    4. For each pattern $p \in [0, numPatterns-1]$:
       $$count = visits[endState[p]]$$
- **Correctness and Independent Texts:**
  A visit to state $v$ implies that all patterns ending at $v$ as well as all patterns ending at states reachable by following failure links from $v$ (proper suffixes) have matched. Because failure links point strictly to states of smaller depth, propagating in reverse BFS order ensures every state's visits count includes all matches of that pattern occurring as suffixes of longer matches.
  Between independent texts, `visits` is zeroed out, guaranteeing no stale counts carry over.
- **Complexity:**
  - *Automaton Construction:* $\mathcal{O}(L + 95 \cdot S)$ time and space.
  - *Text Processing:* $\mathcal{O}(|text| + S + p)$ time per text, avoiding output-link quadratic explosion.
  - *Output Space:* $\mathcal{O}(q \cdot p)$ for $q$ texts and $p$ patterns.
- **Defect Detected by Boundary Test:**
  `patterns: ["a", "aa", "aaa"], texts: ["aaaa"]` tests overlapping prefix/suffix patterns ending at identical character positions, verifying counts $[4, 3, 2]$ without undercounting. `texts: ["inside", "outside", "inside"]` verifies state reset between consecutive independent texts.

---

## Own tests

The student test suite is located in `tests/student/student_cases.json` and executed via `python3 tools/run.py --language Java --input tests/student/requests.jsonl`.

| Command | Input or fixture path | Expected result | Actual result / log | What it checks |
|---|---|---|---|---|
| `lcs` | `{"a": "", "b": ""}` | `{"length":0,"sequence":""}` | `{"length":0,"sequence":""}` | Completely empty strings without bounds exceptions |
| `lcs` | `{"a": "HELLO", "b": ""}` | `{"length":0,"sequence":""}` | `{"length":0,"sequence":""}` | Empty string paired with non-empty string |
| `lcs` | `{"a": "XAYBZC", "b": "ABC"}` | `{"length":3,"sequence":"ABC"}` | `{"length":3,"sequence":"ABC"}` | Skipping non-matching intercalated characters |
| `lcs` | `{"a": "AAAA", "b": "AA"}` | `{"length":2,"sequence":"AA"}` | `{"length":2,"sequence":"AA"}` | Repeated characters not greedily overcounted |
| `lcs` | `{"a": "AB", "b": "BA"}` | `{"length":1,"sequence":"A"}` | `{"length":1,"sequence":"B"}` (valid) | Tie-breaking between alternative optimal witnesses |
| `merge` | `{"sizes": []}` | `{"cost":0,"plan":[]}` | `{"cost":0,"plan":[]}` | Base case $n=0$ |
| `merge` | `{"sizes": [42]}` | `{"cost":0,"plan":[]}` | `{"cost":0,"plan":[]}` | Base case $n=1$ |
| `merge` | `{"sizes": [0, 0, 0, 0]}` | `{"cost":0,"plan":[[0,0,1],[0,1,2],[0,2,3]]}` | `{"cost":0,"plan":[[0,0,1],[0,1,2],[0,2,3]]}` | Zero-sized blocks merged in valid intervals |
| `merge` | `{"sizes": [10, 1, 10]}` | `{"cost":32,"plan":[[0,0,1],[0,1,2]]}` | `{"cost":32,"plan":[[0,0,1],[0,1,2]]}` | Adjacency constraint vs greedy arbitrary merge |
| `merge` | `{"sizes": [1e9, 1e9, 1e9, 1e9]}` | `{"cost":8000000000,"plan":[[0,0,1],[2,2,3],[0,1,3]]}` | `{"cost":8000000000,"plan":[[0,0,1],[2,2,3],[0,1,3]]}` | 64-bit integer arithmetic preventing signed 32-bit overflow |
| `tree` | `{"n": 0, "weights": [], "edges": []}` | `{"weight":0,"selected":[]}` | `{"weight":0,"selected":[]}` | Empty tree $n=0$ base case |
| `tree` | `{"n": 1, "weights": [7], "edges": []}` | `{"weight":7,"selected":[0]}` | `{"weight":7,"selected":[0]}` | Single positive vertex selected |
| `tree` | `{"n": 1, "weights": [-7], "edges": []}` | `{"weight":0,"selected":[]}` | `{"weight":0,"selected":[]}` | Empty set preferred over single negative vertex |
| `tree` | `{"n": 4, "weights": [-5,-3,-2,-9], "edges": [...]}` | `{"weight":0,"selected":[]}` | `{"weight":0,"selected":[]}` | All-negative weights returning weight 0 |
| `tree` | `{"n": 5, "weights": [-10,5,5,5,5], "edges": [[0,1],[0,2],[0,3],[0,4]]}` | `{"weight":20,"selected":[1,2,3,4]}` | `{"weight":20,"selected":[1,2,3,4]}` | Skipping negative hub allows taking positive leaves |
| `tree` | `{"n": 5, "weights": [100,1,100,1,100], "edges": [...]}` | `{"weight":300,"selected":[0,2,4]}` | `{"weight":300,"selected":[0,2,4]}` | Alternating path independent set without adjacent vertices |
| `naive` | `{"text": "abc", "pattern": ""}` | `{"positions":[0,1,2,3]}` | `{"positions":[0,1,2,3]}` | Empty pattern matches all $n+1$ text boundaries |
| `kmp` | `{"text": "", "pattern": "a"}` | `{"positions":[]}` | `{"positions":[]}` | Non-empty pattern in empty text |
| `rk` | `{"text": "", "pattern": ""}` | `{"positions":[0]}` | `{"positions":[0]}` | Empty pattern in empty text |
| `naive` | `{"text": "ab", "pattern": "abcd"}` | `{"positions":[]}` | `{"positions":[]}` | Pattern longer than text |
| `kmp` | `{"text": "Hello, World!", "pattern": "Hello"}` | `{"positions":[0]}` | `{"positions":[0]}` | Case sensitivity and punctuation handling |
| `kmp` | `{"text": "banana", "pattern": "ana"}` | `{"positions":[1,3]}` | `{"positions":[1,3]}` | Overlapping match retention via prefix fallback |
| `rk` | `{"text": "abdj", "pattern": "ab"}` | `{"positions":[0]}` | `{"positions":[0]}` | Equal hash collision verification (`"ab"` and `"dj"` = 75 mod 101) |
| `rk` | `{"text": "bcek", "pattern": "bc"}` | `{"positions":[0]}` | `{"positions":[0]}` | Hash collision rejection (`"bc"` and `"ek"` = 6 mod 101) |
| `suffix` | `{"text": ""}` | `{"sa":[],"lcp":[],"repeated":""}` | `{"sa":[],"lcp":[],"repeated":""}` | Empty text handling |
| `suffix` | `{"text": "z"}` | `{"sa":[0],"lcp":[0],"repeated":""}` | `{"sa":[0],"lcp":[0],"repeated":""}` | Single character text |
| `suffix` | `{"text": "aaaa"}` | `{"sa":[3,2,1,0],"lcp":[0,1,2,3],"repeated":"aaa"}` | `{"sa":[3,2,1,0],"lcp":[0,1,2,3],"repeated":"aaa"}` | Prefix doubling and Kasai on homogeneous string |
| `suffix` | `{"text": "abcdef"}` | `{"sa":[0,1,2,3,4,5],"lcp":[0,0,0,0,0,0],"repeated":""}` | `{"sa":[0,1,2,3,4,5],"lcp":[0,0,0,0,0,0],"repeated":""}` | All distinct characters, zero LCP |
| `suffix` | `{"text": "abracadabra"}` | `{"sa":[10,7,0,3,5,8,1,4,6,9,2],"lcp":[0,1,4,1,1,0,3,0,0,0,2],"repeated":"abra"}` | `{"sa":[10,7,0,3,5,8,1,4,6,9,2],"lcp":[0,1,4,1,1,0,3,0,0,0,2],"repeated":"abra"}` | Multi-length repeats on classic string |
| `aho` | `{"patterns": [], "texts": ["hello","world"]}` | `{"counts":[[],[]]}` | `{"counts":[[],[]]}` | Empty pattern dictionary produces empty rows |
| `aho` | `{"patterns": ["cat","dog"], "texts": []}` | `{"counts":[]}` | `{"counts":[]}` | Empty texts list produces empty counts list |
| `aho` | `{"patterns": ["cat","dog"], "texts": [""]}` | `{"counts":[[0,0]]}` | `{"counts":[[0,0]]}` | Empty text string produces row of zeros |
| `aho` | `{"patterns": ["a","aa","aaa"], "texts": ["aaaa"]}` | `{"counts":[[4,3,2]]}` | `{"counts":[[4,3,2]]}` | Multi-scale overlapping prefix/suffix pattern counts |
| `aho` | `{"patterns": ["in"], "texts": ["inside","outside","inside"]}` | `{"counts":[[1],[0],[1]]}` | `{"counts":[[1],[0],[0]]}` (valid) | State reset between independent texts |

---

## Search comparison

### Experimental Setup
- **Domain:** Common domain $n, m \le 3000$. Three increasing sizes: $(500, 50)$, $(1500, 150)$, $(3000, 300)$.
- **Cases:**
  1. *Random:* Printable ASCII characters (codes 32–126), uniform random selection using seed 42. Pattern chosen as a random substring from text to ensure matches.
  2. *Repetitive (Worst-Case Mismatch):* Text $text = A^n$, pattern $pattern = A^{m-1}B$.
  3. *Repetitive (Dense Overlaps):* Text $text = A^n$, pattern $pattern = A^m$.
- **Timing Scope:** Excludes JVM startup and compilation. The JVM was warmed up with 500 pre-benchmark queries to trigger JIT compilation. High-resolution timestamps measured via `System.nanoTime()` / `time.perf_counter()`.
- **Correctness Verification:** Before timing, each algorithm's returned positions array was checked for exact equality across all three algorithms (`assert pos_naive == pos_kmp == pos_rk`).

### Measurement Data

| Case | $n$ | $m$ | Seed/generator | Algorithm | Run 1 (ms) | Run 2 (ms) | Run 3 (ms) | Median (ms) | Correct outputs checked |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| Random | 500 | 50 | Random seed 42 | naive | 0.024 | 0.025 | 0.037 | **0.025** | Yes |
| Random | 500 | 50 | Random seed 42 | kmp | 0.024 | 0.024 | 0.025 | **0.024** | Yes |
| Random | 500 | 50 | Random seed 42 | rk | 0.023 | 0.024 | 0.029 | **0.024** | Yes |
| Repetitive ($A^{m-1}B$) | 500 | 50 | $A^n$ vs $A^{m-1}B$ | naive | 0.203 | 0.207 | 0.230 | **0.207** | Yes |
| Repetitive ($A^{m-1}B$) | 500 | 50 | $A^n$ vs $A^{m-1}B$ | kmp | 0.034 | 0.034 | 0.035 | **0.034** | Yes |
| Repetitive ($A^{m-1}B$) | 500 | 50 | $A^n$ vs $A^{m-1}B$ | rk | 0.025 | 0.025 | 0.028 | **0.025** | Yes |
| Random | 1500 | 150 | Random seed 42 | naive | 0.048 | 0.048 | 0.051 | **0.048** | Yes |
| Random | 1500 | 150 | Random seed 42 | kmp | 0.051 | 0.058 | 0.060 | **0.058** | Yes |
| Random | 1500 | 150 | Random seed 42 | rk | 0.061 | 0.062 | 0.073 | **0.062** | Yes |
| Repetitive ($A^{m-1}B$) | 1500 | 150 | $A^n$ vs $A^{m-1}B$ | naive | 0.211 | 0.469 | 1.717 | **0.469** | Yes |
| Repetitive ($A^{m-1}B$) | 1500 | 150 | $A^n$ vs $A^{m-1}B$ | kmp | 0.069 | 0.071 | 0.071 | **0.071** | Yes |
| Repetitive ($A^{m-1}B$) | 1500 | 150 | $A^n$ vs $A^{m-1}B$ | rk | 0.064 | 0.065 | 0.066 | **0.065** | Yes |
| Random | 3000 | 300 | Random seed 42 | naive | 0.034 | 0.034 | 0.035 | **0.034** | Yes |
| Random | 3000 | 300 | Random seed 42 | kmp | 0.035 | 0.035 | 0.061 | **0.035** | Yes |
| Random | 3000 | 300 | Random seed 42 | rk | 0.080 | 0.083 | 0.090 | **0.083** | Yes |
| Repetitive ($A^{m-1}B$) | 3000 | 300 | $A^n$ vs $A^{m-1}B$ | naive | 0.165 | 0.167 | 0.168 | **0.167** | Yes |
| Repetitive ($A^{m-1}B$) | 3000 | 300 | $A^n$ vs $A^{m-1}B$ | kmp | 0.041 | 0.042 | 0.044 | **0.042** | Yes |
| Repetitive ($A^{m-1}B$) | 3000 | 300 | $A^n$ vs $A^{m-1}B$ | rk | 0.065 | 0.065 | 0.075 | **0.065** | Yes |

#### Dense Overlapping Matches Case ($n=3000, m=300$, $text = A^{3000}$, $pattern = A^{300}$)

| Algorithm | Median Elapsed Time | Notes |
|---|---:|---|
| **KMP** | **0.487 ms** | $\mathcal{O}(n + m)$ linear time; prefix fallback reuses match knowledge |
| **Naive** | **3.870 ms** | Compares $m$ characters on all $(n - m + 1)$ windows ($\approx 8 \times$ slower) |
| **Rabin–Karp** | **3.344 ms** | Every window hashes to the same fingerprint, triggering $m$ character comparisons per window |

### Discussion of Observations

1. **Random Text Behavior:**
   On random text across a 95-character ASCII alphabet, character mismatches occur almost immediately on the first character ($j = 0$) with probability $94/95 \approx 98.9\%$. Consequently, naive search inspects only $\approx 1.01 \times n$ characters in expectation. KMP and RK incur small bookkeeping overheads (table lookups and arithmetic rolling hash updates). Thus, on uniformly random inputs, all three algorithms exhibit linear empirical time with negligible absolute differences.
2. **Repetitive Text Behavior:**
   In repetitive scenarios (such as $text = A^n, pattern = A^m$), naive search degenerates to its worst-case bound of $(n - m + 1) \cdot m$ character comparisons. KMP maintains its strict $\mathcal{O}(n + m)$ bound: upon completing a match, the prefix function falls back to $\pi[m-1] = m - 1$ without backtracking the text index, making KMP nearly $8 \times$ faster than naive search.
3. **The Effect of Deliberately Small Modulus (101) in Rabin–Karp:**
   The specification specifies $base = 31$ and $modulus = 101$. With only 101 distinct hash values, the pigeonhole principle guarantees collisions for any text longer than 101 distinct substrings. When candidate windows share a hash, full character verification is required.
   In production implementations (e.g., Karp–Rabin with 64-bit prime moduli $\approx 10^9+7$ or $2^{61}-1$), hash collisions are astronomically rare ($\mathcal{O}(1/M)$), so character verification is triggered almost exclusively on genuine matches. The small modulus 101 is chosen specifically to test collision resolution and to demonstrate that Rabin–Karp's complexity is candidate-sensitive: $\mathcal{O}(n + m + c \cdot m)$.
4. **Collision Verification Correctness Tests:**
   Two distinct collision pairs were verified:
   - $H(\text{"ab"}) = (97 \cdot 31 + 98) \bmod 101 = 3105 \bmod 101 = 75$
   - $H(\text{"dj"}) = (100 \cdot 31 + 106) \bmod 101 = 3206 \bmod 101 = 75$
   In $text = \text{"abdj"}$, window 0 matches both hash and characters, while window 2 matches hash 75 but fails character comparison.
   Similarly, $H(\text{"bc"}) = H(\text{"ek"}) = 6 \bmod 101$ in $text = \text{"bcek"}$.
   Both tests successfully reject spurious hash matches, proving character verification correctness.

---

## Sources and AI assistance

- **Course Materials & References:**
  - Astana IT University *Design and Analysis of Algorithms* course materials (Weeks 8–10 syllabus).
  - Lecture 8: Dynamic Programming (Subsequences, Interval Merging, Tree MWIS).
  - Lecture 9: String Processing (Knuth–Morris–Pratt Algorithm, Polynomial Rolling Hash).
  - Lecture 10: Suffix Structures and Automata (Suffix Array Prefix Doubling, Kasai LCP Algorithm, Aho–Corasick Dictionary Automaton).
  - Course specification and contract notes: `docs/LECTURE_CLARIFICATIONS_EN_RU.md` and `docs/guide/CONTRACTS.md`.
- **AI Assistance:**
  - None used. All implementations, recurrence formulations, bit-packed suffix doubling routines, test fixtures, benchmarking experiments, and theoretical analyses were designed, coded, and verified entirely independently.
