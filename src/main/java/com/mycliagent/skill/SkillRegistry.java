package com.mycliagent.skill;

import java.util.List;

public class SkillRegistry {
    public List<Skill> enabledSkills() {
        return List.of();
    }

    public Skill findSkill(String name) {
        return enabledSkills().stream()
                .filter(skill -> skill.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Skill findAnySkill(String name) {
        return findSkill(name);
    }
}
