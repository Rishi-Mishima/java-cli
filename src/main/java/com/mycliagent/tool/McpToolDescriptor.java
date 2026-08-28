package com.mycliagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record McpToolDescriptor(String serverName, String name, String description, JsonNode inputSchema) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public McpToolDescriptor {
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName 不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        if (description == null) {
            description = "";
        }
        if (inputSchema == null) {
            inputSchema = MAPPER.createObjectNode().put("type", "object");
        }
    }

    public String namespacedName() {
        return "mcp__" + serverName + "__" + name;
    }
}
