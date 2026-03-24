This software accompanies this paper:

Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis

Atieh Barati Nia, Mohammad Dindoost, David A. Bader

Large Language Models (LLMs) are increasingly used to automate software development, yet most prior evaluations focus on functional correctness or high-level languages such as Python. We present the first systematic study of LLMs' ability to generate efficient C implementations of graph-analysis routines--code that must satisfy the stringent runtime and memory constraints. Eight state-of-the-art models (OpenAI ChatGPT o3 and o4-mini-high, Anthropic Claude 4 Sonnet and Sonnet Extended, Google Gemini 2.5 Flash and Pro, xAI Grok 3-Think, and DeepSeek DeepThink R1) are benchmarked by two distinct approaches. The first approach checks the ability of LLMs in generating an algorithm outperforming other present algorithms in the benchmark. The second approach evaluates the ability of LLMs to generate graph algorithms for integration into the benchmark. Results show that Claude Sonnet 4 Extended achieves the best result in the case of ready-to-use code generation and efficiency, outperforming human-written baselines in triangle counting. The study confirms that contemporary LLMs excel at optimizing and integrating established algorithms but not inventing novel techniques. We provide prompts, the first approach's generated code, and measurement scripts to foster reproducible research.

Arxiv:
https://doi.org/10.48550/arXiv.2507.06463

``Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis,'' 
Atieh Barati Nia, Mohammad Dindoost, and David A. Bader, 
The 29th Annual IEEE High Performance Extreme Computing Conference (HPEC), 
Virtual, September 15-19, 2025.
