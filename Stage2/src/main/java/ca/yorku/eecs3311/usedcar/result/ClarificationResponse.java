package ca.yorku.eecs3311.usedcar.result;
/** Pending conflicts carry values for review; they are not applied until an explicit decision. */
public record ClarificationResponse(String status,String answer,String existing,String proposed) { }
