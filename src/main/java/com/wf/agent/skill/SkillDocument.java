package com.wf.agent.skill;

import lombok.Data;

import java.util.Map;

/**
 * Skill 文档对象
 *
 * 封装 SKILL.md 的解析结果，包含元数据和正文内容
 *
 * @author author
 * @since 1.0.0
 */
@Data
public class SkillDocument {

    /** Skill 名称 */
    private String name;

    /** Skill 描述 */
    private String description;

    /** 其他元数据 */
    private Map<String, String> metadata;

    /** Markdown 正文内容 */
    private String content;

    /** Skill 目录路径 */
    private String path;

    /**
     * 获取完整的 Skill 文本（元数据 + 正文）
     */
    public String getFullText() {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(name).append("\n");
        sb.append("description: ").append(description).append("\n");
        if (metadata != null && !metadata.isEmpty()) {
            metadata.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        }
        sb.append("---\n\n");
        sb.append(content);
        return sb.toString();
    }

    /**
     * 获取判定规则部分的内容
     */
    public String getRulesSection() {
        if (content == null) return "";

        // 提取 "## 判定规则" 部分
        int start = content.indexOf("## 判定规则");
        if (start == -1) return "";

        int end = content.indexOf("##", start + 1);
        if (end == -1) end = content.length();

        return content.substring(start, end).trim();
    }
}
