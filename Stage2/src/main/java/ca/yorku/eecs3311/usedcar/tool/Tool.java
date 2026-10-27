package ca.yorku.eecs3311.usedcar.tool;

/** Stage 1 tool contract. Each implementation checks its request type before execution. */
public interface Tool { Object execute(Object input); }
