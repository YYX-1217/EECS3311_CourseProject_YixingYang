package ca.yorku.eecs3311.usedcar.command;

/** Typed refinement of Stage 1 execute(): Object. */
@FunctionalInterface
public interface Command<R> { R execute(); }
