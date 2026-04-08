package com.wf.agent.skill;

import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Skills Agent Hook
 *
 * 职责：
 * 连接智能体与 Skill 系统，提供工具调用能力
 * 让 Agent 能够动态发现和加载 Skill
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillsAgentHook {

    private final SkillRegistry skillRegistry;

    /**
     * 工具：列出所有可用 Skill
     *
     * 供模型在初始阶段了解有哪些 Skill 可用
     */
    @Tool(name = "list_skills", description = "列出所有可用的 Skill 及其描述")
    public String listSkills() {
        log.info("[SkillsAgentHook] 列出所有 Skill");
        return skillRegistry.getSkillListForModel();
    }

    /**
     * 工具：读取指定 Skill 的完整内容
     *
     * 模拟 read_skill()，模型判断需要某个 Skill 时调用
     */
    @Tool(name = "read_skill", description = "读取指定 Skill 的完整内容，包括判定规则和使用方法")
    public String readSkill(
            @ToolParam(description = "Skill 名称，如 disaster-rainstorm-assessment") String skillName) {
        log.info("[SkillsAgentHook] 读取 Skill: {}", skillName);
        return skillRegistry.readSkill(skillName);
    }

    /**
     * 工具：读取指定 Skill 的判定规则
     *
     * 用于只需要规则部分的场景
     */
    @Tool(name = "read_skill_rules", description = "读取指定 Skill 的判定规则部分")
    public String readSkillRules(
            @ToolParam(description = "Skill 名称") String skillName) {
        log.info("[SkillsAgentHook] 读取 Skill 规则: {}", skillName);
        return skillRegistry.readSkillRules(skillName);
    }

    /**
     * 工具：根据灾害类型自动匹配并读取 Skill
     *
     * 简化调用，自动映射灾害类型到 Skill
     */
    @Tool(name = "get_disaster_skill", description = "根据灾害类型自动获取对应的 Skill 规则")
    public String getDisasterSkill(
            @ToolParam(description = "灾害类型，如暴雨、大风、雷电等") String disasterType) {
        log.info("[SkillsAgentHook] 获取灾害 Skill: {}", disasterType);

        SkillDocument doc = skillRegistry.getSkillByDisasterType(disasterType);
        if (doc == null) {
            return "未找到灾害类型 '" + disasterType + "' 对应的 Skill";
        }

        return doc.getRulesSection();
    }

    /**
     * 将 Skill 工具注入到 State 中
     *
     * 供 Node 使用，让 Node 能访问 Skill 系统
     */
    public void injectSkillsToState(OverAllState state) {
        Map<String, Object> skillContext = new HashMap<>();
        skillContext.put("availableSkills", skillRegistry.getAllSkillNames());
        skillContext.put("skillCount", skillRegistry.getSkillCount());

        // 将 Skill 上下文放入 State
        Map<String, Object> data = state.data();
        data.put("_skillContext", skillContext);

        log.debug("[SkillsAgentHook] Skill 上下文已注入 State");
    }

    /**
     * 从 State 中获取 Skill 上下文
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSkillContextFromState(OverAllState state) {
        Object context = state.data().get("_skillContext");
        if (context instanceof Map) {
            return (Map<String, Object>) context;
        }
        return new HashMap<>();
    }
}
