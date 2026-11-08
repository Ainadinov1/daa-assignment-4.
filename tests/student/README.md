# Student Tests for Assignment 4

This directory contains student test fixtures and test cases across the full published input domains for all 8 commands:
- `student_cases.json`: 34 comprehensive test cases with names, inputs, expected outputs, and descriptions of what defect each test detects.
- `requests.jsonl`: JSON Lines formatted requests for execution with `python tools/run.py --language Java --input tests/student/requests.jsonl`.

### Test Summary
- **LCS**: empty strings (both/single), skipping non-matching intercalated characters, repeated characters, alternative optimal witness tie-breaking.
- **Merge**: 0/1 blocks, zero-size blocks, adjacency constraint vs greedy arbitrary choice, large 64-bit costs, chronological valid plan structure.
- **Tree**: 0/1 vertices, negative single vertex, all-negative graph (empty set of weight 0), negative hub with positive leaves, alternating chain.
- **Naive / KMP / RK**: empty pattern matching all $n+1$ boundaries, empty text, both empty, pattern longer than text, case-sensitivity and punctuation, overlapping occurrences.
- **Rabin-Karp**: rolling hash arithmetic, negative remainder normalization modulo 101, hash collisions (`"ab"` vs `"dj"` at hash 75, `"bc"` vs `"ek"` at hash 6) requiring character verification.
- **Suffix Array & Kasai LCP**: empty string, single character, all identical characters (`"aaaa"`), all distinct characters (`"abcdef"`), repeating substring with distinct starts (`"abracadabra"` -> `"abra"`).
- **Aho-Corasick**: empty pattern dictionary, empty texts array, empty text string, multi-scale overlapping prefix/suffix patterns (`"a"`, `"aa"`, `"aaa"`), independent text state resetting.
