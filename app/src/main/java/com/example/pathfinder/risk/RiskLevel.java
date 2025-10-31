package com.example.pathfinder.risk;

// Níveis de risco para análise de colisão
public enum RiskLevel {
    SAFE(0, "Seguro"),
    LOW(1, "Baixo risco"),
    MEDIUM(2, "Risco médio"),
    HIGH(3, "Risco alto"),
    CRITICAL(4, "Risco crítico");

    private final int priority;
    private final String description;

    RiskLevel(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

/*************  ✨ Windsurf Command ⭐  *************/
    /**
     * Returns the priority of the risk level, with higher values indicating a
     * greater risk.
     * @return the priority of the risk level
     */

/*******  13162c2e-46f9-4e5e-a1bf-65a583549af2  *******/    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }
}