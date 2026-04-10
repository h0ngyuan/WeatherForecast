package com.wf.agent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Skill 加载器
 *
 * 负责从文件系统加载 .trae/skills/ 目录下的所有 Skill
 * 支持热重载和缓存
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class SkillLoader {

    private static final String SKILL_ROOT = ".trae/skills";
    private static final String SKILL_FILE = "SKILL.md";

    /** Skill 缓存：name -> SkillDocument */
    private final Map<String, SkillDocument> skillCache = new ConcurrentHashMap<>();

    /** 项目根目录 */
    private Path projectRoot;

    @PostConstruct
    public void init() {
        // 尝试找到项目根目录
        projectRoot = findProjectRoot();
        if (projectRoot != null) {
            log.info("[SkillLoader] 项目根目录: {}", projectRoot);
            loadAllSkills();
        } else {
            log.error("[SkillLoader] 无法找到项目根目录");
        }
    }

    /**
     * 查找项目根目录（包含 .trae 目录）
     */
    private Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(SKILL_ROOT))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 加载所有 Skill
     */
    public void loadAllSkills() {
        Path skillsDir = projectRoot.resolve(SKILL_ROOT);
        if (!Files.exists(skillsDir)) {
            log.warn("[SkillLoader] Skill 目录不存在: {}", skillsDir);
            return;
        }

        try (Stream<Path> paths = Files.list(skillsDir)) {
            paths.filter(Files::isDirectory)
                    .forEach(this::loadSkillFromDir);
            log.info("[SkillLoader] 共加载 {} 个 Skill", skillCache.size());
        } catch (IOException e) {
            log.error("[SkillLoader] 加载 Skill 失败", e);
        }
    }

    /**
     * 从目录加载单个 Skill
     */
    private void loadSkillFromDir(Path skillDir) {
        Path skillFile = skillDir.resolve(SKILL_FILE);
        if (!Files.exists(skillFile)) {
            log.warn("[SkillLoader] Skill 文件不存在: {}", skillFile);
            return;
        }

        try {
            String content = Files.readString(skillFile);
            SkillDocument doc = parseSkillDocument(content);
            doc.setPath(skillDir.toString());
            String skillName = doc.getName();
            if (skillName == null || skillName.isEmpty()) {
                skillName = skillDir.getFileName().toString();
                doc.setName(skillName);
            }
            skillCache.put(skillName, doc);
            log.info("[SkillLoader] 加载 Skill: {} - {}", skillName, doc.getDescription());
        } catch (IOException e) {
            log.error("[SkillLoader] 读取 Skill 文件失败: {}", skillFile, e);
        }
    }

    /**
     * 解析 Skill 文档
     */
    private SkillDocument parseSkillDocument(String content) {
        SkillDocument doc = new SkillDocument();

        // 解析 Frontmatter
        Pattern frontmatterPattern = Pattern.compile("^---\\n(.*?)\\n---\\n(.*)$", Pattern.DOTALL);
        Matcher matcher = frontmatterPattern.matcher(content);

        if (matcher.find()) {
            String frontmatter = matcher.group(1);
            String body = matcher.group(2);

            // 解析元数据
            Map<String, String> metadata = new HashMap<>();
            Pattern metaPattern = Pattern.compile("^(\\w+):\\s*(.+)$", Pattern.MULTILINE);
            Matcher metaMatcher = metaPattern.matcher(frontmatter);

            while (metaMatcher.find()) {
                String key = metaMatcher.group(1);
                String value = metaMatcher.group(2).trim();
                if ("name".equals(key)) {
                    doc.setName(value);
                } else if ("description".equals(key)) {
                    doc.setDescription(value);
                } else {
                    metadata.put(key, value);
                }
            }
            doc.setMetadata(metadata);
            doc.setContent(body.trim());
        } else {
            // 没有 frontmatter，整个内容作为正文
            doc.setContent(content);
        }

        return doc;
    }

    /**
     * 根据名称获取 Skill
     */
    public SkillDocument getSkill(String name) {
        return skillCache.get(name);
    }

    /**
     * 获取所有 Skill 名称
     */
    public Set<String> getAllSkillNames() {
        return new HashSet<>(skillCache.keySet());
    }

    /**
     * 获取所有 Skill 元数据（用于模型选择）
     */
    public List<SkillMeta> getAllSkillMetas() {
        List<SkillMeta> metas = new ArrayList<>();
        for (SkillDocument doc : skillCache.values()) {
            metas.add(new SkillMeta(doc.getName(), doc.getDescription()));
        }
        return metas;
    }

    /**
     * 根据灾害类型获取对应的 Skill
     */
    public SkillDocument getSkillByDisasterType(String disasterType) {
        String skillName = mapDisasterTypeToSkillName(disasterType);
        return getSkill(skillName);
    }

    /**
     * 灾害类型映射到 Skill 名称
     */
    private String mapDisasterTypeToSkillName(String disasterType) {
        // 空值检查
        if (disasterType == null || disasterType.trim().isEmpty()) {
            log.warn("[SkillLoader] 灾害类型为空，无法映射到Skill");
            return "disaster-risk-assessment"; // 返回通用评估Skill
        }
        
        String type = disasterType.trim();
        
        // 降雨相关（包含小雨、中雨、大雨、暴雨等）
        if (type.contains("雨") || type.contains("降雨") || type.contains("Rain")) {
            return "disaster-rainstorm-assessment";
        } else if (type.contains("风") || type.contains("Wind")) {
            return "disaster-wind-assessment";
        } else if (type.contains("雷") || type.contains("Thunder") || type.contains("电")) {
            return "disaster-thunder-assessment";
        } else if (type.contains("冰雹") || type.contains("Hail")) {
            return "disaster-hail-assessment";
        } else if (type.contains("雪") || type.contains("Snow")) {
            return "disaster-snow-assessment";
        } else if (type.contains("雾") || type.contains("Fog") || type.contains("霾") || type.contains("沙尘")) {
            return "disaster-fog-assessment";
        } else if (type.contains("温") || type.contains("热") || type.contains("寒") || type.contains("Temperature")) {
            return "disaster-temperature-assessment";
        }
        
        // 默认返回通用灾害评估Skill，避免返回null导致NPE
        log.warn("[SkillLoader] 未找到灾害类型 '{}' 对应的Skill，使用默认Skill", disasterType);
        return "disaster-risk-assessment";
    }

    /**
     * Skill 元数据（用于模型选择）
     */
    public record SkillMeta(String name, String description) {}
}
