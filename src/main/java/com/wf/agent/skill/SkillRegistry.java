package com.wf.agent.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;

/**
 * Skill 注册中心
 *
 * 职责：
 * 1. 管理所有已加载的 Skill
 * 2. 提供 Skill 发现和查询能力
 * 3. 为 Agent 提供 Skill 元数据列表
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final SkillLoader skillLoader;

    @PostConstruct
    public void init() {
        log.info("[SkillRegistry] Skill 注册中心初始化完成，共 {} 个 Skill",
                getSkillCount());
    }

    /**
     * 获取 Skill 数量
     */
    public int getSkillCount() {
        return skillLoader.getAllSkillNames().size();
    }

    /**
     * 获取所有 Skill 名称
     */
    public Set<String> getAllSkillNames() {
        return skillLoader.getAllSkillNames();
    }

    /**
     * 根据名称获取 Skill
     */
    public SkillDocument getSkill(String name) {
        return skillLoader.getSkill(name);
    }

    /**
     * 获取所有 Skill 元数据（用于模型选择）
     */
    public List<SkillLoader.SkillMeta> getAllSkillMetas() {
        return skillLoader.getAllSkillMetas();
    }

    /**
     * 获取用于模型选择的 Skill 列表文本
     */
    public String getSkillListForModel() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用 Skill 列表：\n\n");

        List<SkillLoader.SkillMeta> metas = getAllSkillMetas();
        for (SkillLoader.SkillMeta meta : metas) {
            sb.append("- ").append(meta.name()).append(": ")
                    .append(meta.description()).append("\n");
        }

        sb.append("\n使用方式：当需要某个 Skill 时，调用 read_skill(skill_name) 获取详细信息\n");
        return sb.toString();
    }

    /**
     * 根据灾害类型获取对应的 Skill
     */
    public SkillDocument getSkillByDisasterType(String disasterType) {
        return skillLoader.getSkillByDisasterType(disasterType);
    }

    /**
     * 读取指定 Skill 的完整内容
     * 模拟 read_skill() 工具调用
     */
    public String readSkill(String skillName) {
        SkillDocument doc = getSkill(skillName);
        if (doc == null) {
            return "Skill not found: " + skillName;
        }
        return doc.getFullText();
    }

    /**
     * 读取指定 Skill 的判定规则部分
     */
    public String readSkillRules(String skillName) {
        SkillDocument doc = getSkill(skillName);
        if (doc == null) {
            return "Skill not found: " + skillName;
        }
        return doc.getRulesSection();
    }

    /**
     * 根据灾害类型读取 Skill 规则
     */
    public String readSkillRulesByDisasterType(String disasterType) {
        SkillDocument doc = getSkillByDisasterType(disasterType);
        if (doc == null) {
            return "Skill not found for disaster type: " + disasterType;
        }
        return doc.getRulesSection();
    }
}
