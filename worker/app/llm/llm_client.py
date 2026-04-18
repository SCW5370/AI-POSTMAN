from __future__ import annotations

import json
import time
import threading
from typing import Any, List, Dict, Optional, Tuple

import httpx
import json5
from app.config import (
    LLM_API_BASE, LLM_API_KEY, LLM_MODEL, LLM_TIMEOUT_SECONDS,
    LLM_CONNECT_TIMEOUT_SECONDS, LLM_EDITORIAL_TIMEOUT_SECONDS,
    LLM_FINALIZE_TIMEOUT_SECONDS, LLM_EDITORIAL_RETRY_COUNT,
    LLM_FINALIZE_RETRY_COUNT, LLM_CACHE_ENABLED, LLM_CACHE_TTL_SECONDS,
    LLM_TRUST_ENV, LLM_DEBUG
)
from app.utils.cache import cache


class LlmClient:
    def __init__(self) -> None:
        self.base_url = LLM_API_BASE
        self.api_key = LLM_API_KEY
        self.model = LLM_MODEL
        self.timeout = LLM_TIMEOUT_SECONDS
        self.connect_timeout = LLM_CONNECT_TIMEOUT_SECONDS
        self.editorial_timeout = LLM_EDITORIAL_TIMEOUT_SECONDS
        self.finalize_timeout = LLM_FINALIZE_TIMEOUT_SECONDS
        self.editorial_retry_count = LLM_EDITORIAL_RETRY_COUNT
        self.finalize_retry_count = LLM_FINALIZE_RETRY_COUNT
        self.cache_enabled = LLM_CACHE_ENABLED
        self.cache_ttl_seconds = LLM_CACHE_TTL_SECONDS
        self.trust_env = LLM_TRUST_ENV
        self.debug = LLM_DEBUG  # 启用debug模式
        self._last_error: Optional[str] = None
        # 创建HTTP客户端，使用连接池
        self.http_client = httpx.Client(
            timeout=httpx.Timeout(self.timeout, connect=self.connect_timeout),
            trust_env=self.trust_env,
            limits=httpx.Limits(max_connections=100, max_keepalive_connections=20, keepalive_expiry=300),
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            }
        )

    def is_enabled(self) -> bool:
        return bool(self.api_key and self.model)

    def summarize(self, item: Dict) -> Optional[str]:
        prompt = self._summary_prompt(item)
        return self._chat(prompt, max_tokens=120)

    def relevance_reason(self, item: Dict) -> Optional[str]:
        prompt = self._relevance_prompt(item)
        return self._chat(prompt, max_tokens=90)

    def batch_process(self, items: List[Dict], task_type: str) -> List[Optional[str]]:
        """
        批量处理多个项目，减少 LLM 调用次数
        
        Args:
            items: 要处理的项目列表
            task_type: 任务类型，支持 'summarize' 或 'relevance_reason'
            
        Returns:
            处理结果列表
        """
        if not items:
            return []
        
        # 限制批量处理的大小，避免超过 LLM 的输入限制
        max_batch_size = 5
        results = []
        
        for i in range(0, len(items), max_batch_size):
            batch = items[i:i + max_batch_size]
            prompt = self._batch_prompt(batch, task_type)
            response = self._chat(prompt, max_tokens=100 * len(batch), json_mode=True)
            
            if response:
                try:
                    batch_results = self._parse_json_like(response)
                    if batch_results and 'results' in batch_results:
                        results.extend(batch_results['results'])
                    else:
                        # 如果解析失败，回退到单个处理
                        for item in batch:
                            if task_type == 'summarize':
                                results.append(self.summarize(item))
                            else:
                                results.append(self.relevance_reason(item))
                except Exception:
                    # 如果解析失败，回退到单个处理
                    for item in batch:
                        if task_type == 'summarize':
                            results.append(self.summarize(item))
                        else:
                            results.append(self.relevance_reason(item))
            else:
                # 如果 LLM 调用失败，回退到单个处理
                for item in batch:
                    if task_type == 'summarize':
                        results.append(self.summarize(item))
                    else:
                        results.append(self.relevance_reason(item))
        
        return results

    def _batch_prompt(self, items: List[Dict], task_type: str) -> str:
        """
        生成批量处理的提示
        
        Args:
            items: 要处理的项目列表
            task_type: 任务类型
            
        Returns:
            批量处理的提示
        """
        if task_type == 'summarize':
            prompt = "请为以下每个项目生成一条可直接放进邮件里的中文一句话摘要，突出核心事实或结论，不超过70个中文字符，不要空话，不要以\"这篇文章\"开头。\n\n"
            for i, item in enumerate(items):
                prompt += f"项目 {i+1}:\n"
                prompt += f"标题：{item.get('title_clean', '')}\n"
                prompt += f"摘要：{item.get('summary_clean', '')}\n"
                prompt += f"正文：{item.get('content_clean', '')[:600]}\n\n"
            prompt += "请返回 JSON 格式，包含每个项目的结果，例如：{\"results\":[\"摘要1\", \"摘要2\"]}"
        else:  # relevance_reason
            prompt = "请为以下每个项目生成一句\"为什么值得关注\"的中文说明，语气像个人情报简报，不超过50个中文字符，避免泛泛而谈。\n\n"
            for i, item in enumerate(items):
                prompt += f"项目 {i+1}:\n"
                prompt += f"标题：{item.get('title_clean', '')}\n"
                prompt += f"摘要：{item.get('summary_clean', '')}\n"
                prompt += f"标签：{', '.join(item.get('tags', []))}\n\n"
            prompt += "请返回 JSON 格式，包含每个项目的结果，例如：{\"results\":[\"理由1\", \"理由2\"]}"
        
        return prompt

    def editorial_decision(self, payload: Dict) -> Optional[Dict]:
        prompt = self._editorial_prompt(payload)
        content = self._chat(
            prompt,
            max_tokens=800,  # 增加max_tokens值，确保LLM能够返回完整的JSON响应
            json_mode=True,
            timeout_seconds=self.editorial_timeout,
            retry_count=self.editorial_retry_count,
        )
        if not content:
            return None
        parsed = self._parse_json_like(content)
        if parsed is not None and isinstance(parsed, dict) and "decisions" in parsed:
            return parsed
        repaired = self._repair_json_response(content, "decisions")
        if not repaired:
            return None
        return self._parse_json_like(repaired)

    def finalize_digest_items(self, payload: Dict) -> Optional[Dict]:
        prompt = self._finalize_prompt(payload)
        content = self._chat(
            prompt,
            max_tokens=650,
            json_mode=True,
            timeout_seconds=self.finalize_timeout,
            retry_count=self.finalize_retry_count,
        )
        if not content:
            return None
        parsed = self._parse_json_like(content)
        if parsed is not None and isinstance(parsed, dict) and "items" in parsed:
            return parsed
        repaired = self._repair_json_response(content, "items")
        if not repaired:
            return None
        return self._parse_json_like(repaired)

    def get_last_error(self) -> Optional[str]:
        return self._last_error

    def _debug_log(self, message: str) -> None:
        if self.debug:
            print(f"[worker][llm-debug] {message}")

    def _parse_json_like(self, content: str) -> Optional[Dict]:
        try:
            self._debug_log(f"开始解析JSON，原始内容长度: {len(content)}")
            self._debug_log(f"原始内容前100字符: {content[:100]}...")
            
            # 清理内容
            stripped = content.strip()
            self._debug_log(f"清理后内容前100字符: {stripped[:100]}...")
            
            # 处理Markdown代码块
            if stripped.startswith("```"):
                self._debug_log("发现Markdown代码块")
                # 找到代码块结束标记
                end_code_block = stripped.find("```", 3)
                if end_code_block != -1:
                    stripped = stripped[3:end_code_block].strip()
                    self._debug_log(f"代码块内容前100字符: {stripped[:100]}...")
                # 移除json标记
                if stripped.startswith("json\n"):
                    stripped = stripped[5:].strip()
                    self._debug_log(f"移除json标记后: {stripped[:100]}...")
                elif stripped.startswith("json"):
                    stripped = stripped[4:].strip()
                    self._debug_log(f"移除json标记后: {stripped[:100]}...")
            
            # 尝试使用json5解析（支持不规范的JSON格式）
            try:
                self._debug_log("尝试使用json5解析")
                result = json5.loads(stripped)
                self._debug_log("json5解析成功")
                return result
            except Exception as e:
                self._debug_log(f"json5解析失败: {e}")
                
                # 尝试直接解析整个内容
                try:
                    self._debug_log("尝试直接解析整个内容")
                    result = json.loads(stripped)
                    self._debug_log("直接解析成功")
                    return result
                except json.JSONDecodeError as e:
                    self._debug_log(f"直接解析失败: {e}")
                    
                    # 尝试使用更容错的JSON解析方法
                    try:
                        self._debug_log("尝试使用容错解析方法")
                        # 清理JSON字符串
                        clean_json = self._clean_json(stripped)
                        self._debug_log(f"清理后JSON前100字符: {clean_json[:100]}...")
                        result = json.loads(clean_json)
                        self._debug_log("容错解析成功")
                        return result
                    except json.JSONDecodeError as e:
                        self._debug_log(f"容错解析失败: {e}")
                        
                        # 如果直接解析失败，尝试提取JSON对象
                        start = stripped.find("{")
                        end = stripped.rfind("}")
                        self._debug_log(f"找到JSON对象位置: start={start}, end={end}")
                        if start == -1 or end == -1 or end < start:
                            self._last_error = "json_object_not_found"
                            self._debug_log("未找到JSON对象")
                            return None
                        
                        json_extract = stripped[start:end + 1]
                        self._debug_log(f"提取的JSON前100字符: {json_extract[:100]}...")
                        
                        # 尝试使用json5解析提取的JSON
                        try:
                            self._debug_log("尝试使用json5解析提取的JSON")
                            result = json5.loads(json_extract)
                            self._debug_log("提取json5解析成功")
                            return result
                        except Exception as e:
                            self._debug_log(f"提取json5解析失败: {e}")
                            
                            # 尝试解析提取的JSON
                            try:
                                self._debug_log("尝试解析提取的JSON")
                                result = json.loads(json_extract)
                                self._debug_log("提取解析成功")
                                return result
                            except json.JSONDecodeError as e:
                                self._debug_log(f"提取解析失败: {e}")
                                
                                # 尝试使用容错解析方法处理提取的JSON
                                try:
                                    self._debug_log("尝试使用容错解析方法处理提取的JSON")
                                    clean_json = self._clean_json(json_extract)
                                    self._debug_log(f"清理后提取JSON前100字符: {clean_json[:100]}...")
                                    result = json.loads(clean_json)
                                    self._debug_log("提取容错解析成功")
                                    return result
                                except json.JSONDecodeError as e:
                                    self._debug_log(f"提取容错解析失败: {e}")
                                    self._last_error = "json_decode_error"
                                    return None
        except Exception as e:
            self._debug_log(f"解析过程异常: {e}")
            self._last_error = f"parse_error:{e.__class__.__name__}"
            return None
    
    def _clean_json(self, json_str: str) -> str:
        """
        清理JSON字符串，处理常见的格式问题
        """
        # 移除多余的空格和换行
        clean = json_str.replace("\n", " ").replace("\r", " ")
        # 移除首尾的空白字符
        clean = clean.strip()
        # 修复常见的JSON格式问题
        # 1. 移除尾部可能的逗号
        clean = self._remove_trailing_comma(clean)
        # 2. 确保字符串使用双引号
        # 这里可以添加更多的修复逻辑
        return clean
    
    def _remove_trailing_comma(self, json_str: str) -> str:
        """
        移除JSON中的尾部逗号
        """
        # 简单的实现，移除对象和数组末尾的逗号
        import re
        # 移除对象末尾的逗号
        clean = re.sub(r',\s*}', '}', json_str)
        # 移除数组末尾的逗号
        clean = re.sub(r',\s*\]', ']', clean)
        return clean

    def _chat(
        self,
        prompt: str,
        max_tokens: int,
        json_mode: bool = False,
        timeout_seconds: Optional[float] = None,
        retry_count: int = 0,
    ) -> Optional[str]:
        if not self.is_enabled():
            self._last_error = "llm_not_enabled"
            return None
        cache_key = self._cache_key(prompt, max_tokens)
        if self.cache_enabled:
            cached = self._cache_get(cache_key)
            if cached is not None:
                self._last_error = None
                return cached
        request_body: Dict[str, Any] = {
            "model": self.model,
            # JSON 输出任务使用更稳定的零温度，减少格式漂移
            "temperature": 0.0 if json_mode else 0.2,
            "max_tokens": max_tokens,
            "messages": [
                {
                    "role": "system",
                    "content": "你是一个个人情报简报助手。输出必须简洁、具体、可直接放进邮件。",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
        }
        if json_mode:
            request_body["response_format"] = {"type": "json_object"}
        attempts = max(1, retry_count + 1)
        base_timeout = timeout_seconds or self.timeout
        self._debug_log(
            f"chat start model={self.model} json_mode={json_mode} attempts={attempts} "
            f"timeout={base_timeout}s base_url={self.base_url} trust_env={self.trust_env}"
        )
        for attempt in range(1, attempts + 1):
            attempt_timeout = base_timeout * (1 + 0.5 * (attempt - 1))
            attempt_start = time.monotonic()
            try:
                content = self._post_chat(request_body, attempt_timeout)
                if content and self.cache_enabled:
                    self._cache_set(cache_key, content)
                if content is None:
                    self._last_error = "empty_message_content"
                else:
                    self._last_error = None
                self._debug_log(
                    f"chat success attempt={attempt}/{attempts} elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                return content
            except httpx.HTTPStatusError as ex:
                code = ex.response.status_code if ex.response is not None else "unknown"
                if json_mode and code in (400, 422):
                    try:
                        fallback_body = dict(request_body)
                        fallback_body.pop("response_format", None)
                        content = self._post_chat(fallback_body, attempt_timeout)
                        if content and self.cache_enabled:
                            self._cache_set(cache_key, content)
                        if content is None:
                            self._last_error = "empty_message_content_after_json_mode_fallback"
                        else:
                            self._last_error = None
                        self._debug_log(
                            f"chat json-mode fallback success attempt={attempt}/{attempts} "
                            f"elapsed={time.monotonic() - attempt_start:.2f}s"
                        )
                        return content
                    except Exception:
                        pass
                # 上游 5xx / 429 多为临时异常，允许重试避免一次抖动导致整次任务失败
                if isinstance(code, int) and code in (429, 500, 502, 503, 504) and attempt < attempts:
                    self._last_error = f"http_status_{code}_attempt_{attempt}"
                    self._debug_log(
                        f"chat transient http status attempt={attempt}/{attempts} code={code} "
                        f"elapsed={time.monotonic() - attempt_start:.2f}s; retrying"
                    )
                    time.sleep(min(1.5, 0.4 * attempt))
                    continue
                self._last_error = f"http_status_{code}"
                self._debug_log(
                    f"chat http status error attempt={attempt}/{attempts} code={code} "
                    f"elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                return None
            except httpx.TimeoutException:
                self._last_error = f"llm_timeout_{attempt_timeout:.1f}s_attempt_{attempt}"
                self._debug_log(
                    f"chat timeout attempt={attempt}/{attempts} elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                if attempt < attempts:
                    time.sleep(min(1.5, 0.4 * attempt))
                    continue
                return None
            except httpx.ProxyError as ex:
                self._last_error = f"llm_proxy_error:{ex.__class__.__name__}"
                self._debug_log(
                    f"chat proxy error attempt={attempt}/{attempts} elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                if attempt < attempts:
                    time.sleep(min(1.5, 0.4 * attempt))
                    continue
                return None
            except httpx.ConnectError as ex:
                self._last_error = f"llm_connect_error:{ex.__class__.__name__}"
                self._debug_log(
                    f"chat connect error attempt={attempt}/{attempts} elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                if attempt < attempts:
                    time.sleep(min(1.5, 0.4 * attempt))
                    continue
                return None
            except (httpx.HTTPError, KeyError, IndexError, json.JSONDecodeError) as ex:
                self._last_error = f"{ex.__class__.__name__}"
                self._debug_log(
                    f"chat generic error attempt={attempt}/{attempts} error={ex.__class__.__name__} "
                    f"elapsed={time.monotonic() - attempt_start:.2f}s"
                )
                return None
        self._last_error = "llm_timeout"
        return None

    def _repair_json_response(self, raw_content: str, required_root_key: str) -> Optional[str]:
        prompt = (
            "请把下面内容修复为严格 JSON。"
            f"输出必须是一个对象，且包含根键 \"{required_root_key}\"。"
            "不要输出解释文字，不要 Markdown 代码块。\n\n"
            f"待修复内容：\n{raw_content}"
        )
        self._debug_log(f"attempt json repair for key={required_root_key}")
        return self._chat(
            prompt,
            max_tokens=900,
            json_mode=True,
            timeout_seconds=max(self.timeout, 20),
            retry_count=1,
        )

    def _post_chat(self, request_body: Dict[str, Any], read_timeout: Optional[float] = None) -> Optional[str]:
        effective_timeout = read_timeout or self.timeout
        try:
            # 使用共享的HTTP客户端
            response = self.http_client.post(
                f"{self.base_url.rstrip('/')}/chat/completions",
                json=request_body,
                timeout=httpx.Timeout(effective_timeout, connect=self.connect_timeout),
            )
            response.raise_for_status()
            payload: Dict[str, Any] = response.json()
            return (
                payload.get("choices", [{}])[0]
                .get("message", {})
                .get("content", "")
                .strip()
            ) or None
        except Exception:
            # 如果发生错误，尝试使用临时客户端
            with httpx.Client(
                timeout=httpx.Timeout(effective_timeout, connect=self.connect_timeout),
                trust_env=self.trust_env,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                }
            ) as client:
                response = client.post(
                    f"{self.base_url.rstrip('/')}/chat/completions",
                    json=request_body,
                )
                response.raise_for_status()
                payload: dict[str, Any] = response.json()
                return (
                    payload.get("choices", [{}])[0]
                    .get("message", {})
                    .get("content", "")
                    .strip()
                ) or None

    def _cache_key(self, prompt: str, max_tokens: int) -> str:
        return f"{self.model}|{max_tokens}|{prompt}"

    def _cache_get(self, key: str) -> Optional[str]:
        """
        从缓存中获取值
        """
        return cache.get(key)

    def _cache_set(self, key: str, value: str) -> None:
        """
        将值存入缓存
        """
        cache.set(key, value, ttl=self.cache_ttl_seconds)

    def _summary_prompt(self, item: Dict) -> str:
        return (
            "请根据下面内容生成一条专业、简洁的中文一句话摘要，适合直接放进邮件。\n"
            "要求：\n"
            "1. 突出核心事实、关键数据和重要结论\n"
            "2. 语言简洁有力，不超过70个中文字符\n"
            "3. 避免空话和套话\n"
            "4. 不要以'这篇文章'等引导性短语开头\n"
            "5. 保持客观中立的语气\n\n"
            f"标题：{item.get('title_clean', '')}\n"
            f"摘要：{item.get('summary_clean', '')}\n"
            f"正文：{item.get('content_clean', '')[:1200]}"
        )

    def _relevance_prompt(self, item: Dict) -> str:
        return (
            "请根据下面内容生成一句专业、具体的中文说明，解释为什么这条信息值得关注。\n"
            "要求：\n"
            "1. 语气像专业的个人情报简报\n"
            "2. 具体说明信息的价值和重要性\n"
            "3. 避免泛泛而谈和空洞的表述\n"
            "4. 语言简洁有力，不超过50个中文字符\n"
            "5. 突出信息对用户的实际意义\n\n"
            f"标题：{item.get('title_clean', '')}\n"
            f"摘要：{item.get('summary_clean', '')}\n"
            f"标签：{', '.join(item.get('tags', []))}"
        )

    def _editorial_prompt(self, payload: Dict) -> str:
        return (
            "你是个人情报简报的编辑。请根据用户目标和候选内容，给每条候选决定 section、"
            "一个 -5 到 +8 的 scoreAdjustment，以及一句推荐理由。"
            "只返回 JSON，格式为 {\"decisions\":[{\"normalizedItemId\":1,\"section\":\"MUST_READ\",\"scoreAdjustment\":6,\"reason\":\"...\"}]}"
            " section 只能是 MUST_READ、FOCUS_UPDATES、WORTH_SAVING、SURPRISE。\n\n"
            f"输入数据：{json.dumps(payload, ensure_ascii=False)}"
        )

    def _finalize_prompt(self, payload: dict) -> str:
        # 提取用户画像信息（如果存在）
        user_profile = payload.get('userProfile', {})
        if not isinstance(user_profile, dict):
            user_profile = {}
        interests = user_profile.get('interests', {})
        occupation = user_profile.get('occupation', '')
        preferred_topics = user_profile.get('preferredTopics', {})
        
        # 构建用户画像描述
        profile_description = ""
        if occupation:
            profile_description += f"用户职业：{occupation}。"
        if interests:
            if isinstance(interests, dict):
                top_interests = self._top_dict_keys(interests, 3)
                if top_interests:
                    profile_description += f"用户兴趣：{', '.join(top_interests)}。"
            elif isinstance(interests, str):
                profile_description += f"用户兴趣：{interests}。"
        if preferred_topics:
            if isinstance(preferred_topics, dict):
                top_topics = self._top_dict_keys(preferred_topics, 3)
                if top_topics:
                    profile_description += f"用户偏好话题：{', '.join(top_topics)}。"
            elif isinstance(preferred_topics, str):
                profile_description += f"用户偏好话题：{preferred_topics}。"
        
        return (
            "你是专业的日报主编，擅长将复杂信息转化为清晰、有价值的报道。\n"
            "请根据用户画像和目标，为每条信息创建专业、简洁的中文小报道，用户不点链接也能完全理解内容。\n\n"
            "必须返回 JSON 格式：\n"
            "{\"items\":[{\"normalizedItemId\":1,\"shortSummary\":\"...\",\"relevanceReason\":\"...\",\"actionHint\":\"read_now\"}]}\n\n"
            "严格遵循以下规则：\n"
            "1. shortSummary（80-160中文字符）：\n"
            "   - 写出事实主体、关键数据和重要变化\n"
            "   - 语言生动有趣，专业但不生硬\n"
            "   - 结构清晰，逻辑连贯\n"
            "   - 突出信息的核心价值\n"
            "2. relevanceReason（18-45中文字符）：\n"
            "   - 必须结合用户职业、兴趣和目标\n"
            "   - 具体说明信息对用户的实际意义\n"
            "   - 避免泛泛而谈和空洞表述\n"
            "3. actionHint：\n"
            "   - 只能是 read_now / save_for_later / keep_tracking\n"
            "   - 根据信息的重要性和时效性选择合适的提示\n"
            "4. 整体要求：\n"
            "   - 体现对用户职业和兴趣的理解\n"
            "   - 提供有价值的见解和分析\n"
            "   - 保持专业、客观的语气\n"
            "   - 不要输出 JSON 之外的任何内容\n\n"
            f"用户画像：{profile_description}\n\n"
            f"输入数据：{json.dumps(payload, ensure_ascii=False)}"
        )

    def _top_dict_keys(self, data: Dict, limit: int) -> List[str]:
        ranked: List[Tuple[str, float]] = []
        for key, value in data.items():
            try:
                score = float(value)
            except (TypeError, ValueError):
                score = 0.0
            ranked.append((str(key), score))
        ranked.sort(key=lambda item: item[1], reverse=True)
        return [key for key, _ in ranked[:limit]]
