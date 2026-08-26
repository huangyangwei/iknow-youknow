/**
 * 安全 JSON 解析：基于 json-bigint，将超过 JS Number.MAX_SAFE_INTEGER（2^53-1）
 * 的大整数解析为字符串，防止 Snowflake ID 精度丢失。
 *
 * - 小数字（viewCount、likeCount 等）仍解析为 number
 * - 超过安全范围的整数（Snowflake ID 等）解析为 string
 */
import JSONbig from 'json-bigint'

const safeParser = JSONbig({ storeAsString: true })

export const safeJson = {
  parse: <T = unknown>(text: string): T => safeParser.parse(text) as T,
  stringify: (value: unknown): string => safeParser.stringify(value),
}
