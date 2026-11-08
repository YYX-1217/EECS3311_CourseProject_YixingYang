package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.Command;
import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Scanner;

public final class UsedCarCLI {
    private final UsedCarApplicationFacade facade;
    private final Scanner input;
    private final PrintStream output;

    public UsedCarCLI(UsedCarApplicationFacade facade, Scanner input, PrintStream output) {
        this.facade = facade; this.input = input; this.output = output;
    }
    public <R> R submit(Command<R> command) { return command.execute(); }
    public void displayResult(PreparationResult result) { output.println(ResultFormatter.format(result)); }
    public String readCommand() { return input.hasNextLine() ? input.nextLine().strip() : "quit"; }

    public void demo() {
        output.println("EXAMPLE INPUT: fictional vehicle and explicit sample assumptions, not real market data.");
        displayResult(submit(PreparationForm.command(facade, PreparationForm.example())));
    }

    public void run() {
        output.println("AI Used-Car Evaluation Assistant - Stage 2 foundation");
        help();
        while (true) {
            output.print("usedcar> "); output.flush();
            String raw = readCommand();
            String command=raw.toLowerCase(Locale.ROOT);
            try {
                if(command.equals("clarify")||command.startsWith("clarify-")){clarification(raw);continue;}
                switch (command) {
                    case "quit", "exit" -> { return; }
                    case "help", "" -> help();
                    case "demo" -> demo();
                    case "prepare" -> enterPreparation(false);
                    case "check-conflicts" -> enterPreparation(true);
                    case "profile-set" -> enterPreferences();
                    case "profile-show" -> output.println(ResultFormatter.format(facade.preferences()));
                    case "show" -> {
                        output.println(facade.currentVehicle().map(Object::toString).orElse("No vehicle saved yet."));
                        output.println(facade.preferences());
                    }
                    case "knowledge" -> output.println(ResultFormatter.format(facade.retrieveKnowledge()));
                    case "strategy" -> output.println(facade.previewPriorityGuidance());
                    case "llm-test" -> { output.println(facade.testLlmConnection()); output.println(facade.llmStatus()); }
                    case "evaluate" -> output.println(ResultFormatter.format(submit(new ca.yorku.eecs3311.usedcar.command.EvaluateVehicleCommand(facade))));
                    case "status" -> output.println(facade.llmStatus());
                    case "analyze-listing" -> analyzeListing();
                    case "comparison-set" -> enterComparisonVehicle();
                    case "comparison-show" -> {
                        output.println("A: "+facade.currentVehicle().map(Object::toString).orElse("UNAVAILABLE"));
                        output.println("B: "+facade.comparisonVehicle().map(Object::toString).orElse("UNAVAILABLE"));
                        output.println("Common cost assumptions: "+facade.costAssumptions().map(Object::toString).orElse("UNAVAILABLE"));
                    }
                    case "compare" -> output.println(ResultFormatter.format(submit(new ca.yorku.eecs3311.usedcar.command.CompareVehiclesCommand(facade))));
                    case "checklist" -> output.println(ResultFormatter.format(submit(new ca.yorku.eecs3311.usedcar.command.GenerateChecklistCommand(facade))));
                    case "checklist-show" -> output.println(ResultFormatter.format(facade.currentChecklist().orElseThrow(()->new IllegalStateException("No current checklist; run checklist first."))));
                    case "report" ->
                            output.println("PLANNED: this feature is not implemented. " + facade.llmStatus());
                    default -> {
                        if (command.startsWith("checklist-mark ") || command.startsWith("checklist-unmark ")) {
                            String[] parts=command.split("\\s+");
                            if(parts.length!=2)throw new IllegalArgumentException("Use checklist-mark <id> or checklist-unmark <id>.");
                            var saved=facade.currentChecklist().orElseThrow(()->new IllegalStateException("No current checklist; run checklist first."));
                            output.println(ResultFormatter.format(facade.completeChecklistItem(saved.generationId(),parts[1],parts[0].equals("checklist-mark"))));
                        } else if (command.startsWith("strategy ")) {
                            facade.selectStrategy(command.substring("strategy ".length()));
                            output.println(facade.previewPriorityGuidance());
                        } else output.println("Unknown command. Type help.");
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                output.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void enterPreparation(boolean conflictCheck) {
        if (conflictCheck && facade.currentVehicle().isEmpty()) throw new IllegalStateException("Save a manual vehicle first.");
        output.println(conflictCheck
                ? "Enter a second structured record for the saved vehicle (no AI extraction). Saved data will be preserved."
                : "Enter a vehicle, buyer limits and cost assumptions. Successful preparation replaces the saved vehicle.");
        output.println("Blank vehicle fields are missing; UNKNOWN is explicitly uncertain. Type :cancel to cancel.");
        var values = new LinkedHashMap<String, String>();
        for (var field : PreparationForm.FIELDS) {
            if (conflictCheck && !field.section().equals("Vehicle")) continue;
            output.print(field.label() + ": "); output.flush();
            if (!input.hasNextLine()) { output.println("Cancelled at end of input; previous session preserved."); return; }
            String value = input.nextLine();
            if (value.equalsIgnoreCase(":cancel")) { output.println("Cancelled; previous session preserved."); return; }
            values.put(field.key(), value);
        }
        if (conflictCheck) output.println(ResultFormatter.format(facade.checkConflict(PreparationForm.vehicle(facade, values))));
        else displayResult(submit(PreparationForm.command(facade, values)));
    }

    private void enterComparisonVehicle() {
        if(facade.currentVehicle().isEmpty()) throw new IllegalStateException("Save Vehicle A first using prepare or a confirmed listing.");
        output.println("Enter Vehicle B in CAD, km and L/100 km. Vehicle A, preferences and common cost assumptions are preserved.");
        output.println(":keep retains a saved B field; blank clears; :cancel cancels without saving.");
        var values=facade.comparisonVehicle().map(PreparationForm::vehicleValues).orElseGet(LinkedHashMap::new);
        for(var field:PreparationForm.FIELDS) {
            if(!field.section().equals("Vehicle")) continue;
            output.print(field.label()+" [B="+values.getOrDefault(field.key(),"")+"]: "); output.flush();
            if(!input.hasNextLine()) { output.println("Cancelled; saved vehicles preserved."); return; }
            String value=input.nextLine();
            if(value.equalsIgnoreCase(":cancel")) { output.println("Cancelled; saved vehicles preserved."); return; }
            if(!value.equalsIgnoreCase(":keep")) values.put(field.key(),value);
        }
        var vehicle=PreparationForm.comparisonVehicle(facade,values);
        output.println(vehicle); output.println("Save Vehicle B? Type yes; any other input cancels.");
        if(input.hasNextLine() && input.nextLine().strip().equalsIgnoreCase("yes")) {
            facade.saveComparisonVehicle(vehicle); output.println("Vehicle B saved; Vehicle A and preferences preserved.");
        } else output.println("Cancelled; saved vehicles preserved.");
    }

    private void enterPreferences() {
        output.println("Enter all seven preferences. Blank clears to neutral; :keep retains the saved value; :cancel aborts.");
        var values = PreparationForm.preferenceValues(facade.preferences());
        for (var field : PreparationForm.FIELDS) {
            if (!field.section().equals("Preferences")) continue;
            output.print(field.label() + " [saved=" + values.get(field.key()) + "]: "); output.flush();
            if (!input.hasNextLine()) { output.println("Cancelled; previous profile preserved."); return; }
            String value = input.nextLine();
            if (value.equalsIgnoreCase(":cancel")) { output.println("Cancelled; previous profile preserved."); return; }
            if (!value.equalsIgnoreCase(":keep")) values.put(field.key(), value);
        }
        // All parsing and domain validation happens before any session update.
        facade.savePreferences(PreparationForm.preferences(values));
        output.println(ResultFormatter.format(facade.preferences()));
    }

    private void analyzeListing() {
        output.println("Paste listing text; end with a line containing :end. :cancel aborts. This explicitly requests Gemini analysis.");
        var text = new StringBuilder(); boolean ended = false;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.equals(":cancel")) return;
            if (line.equals(":end")) { ended = true; break; }
            text.append(line).append("\n");
            if (text.length() > 16000) throw new IllegalArgumentException("Listing exceeds 16000 characters.");
        }
        if (!ended) { output.println("Cancelled at end of input; saved vehicle unchanged."); return; }
        var analysis = submit(new ca.yorku.eecs3311.usedcar.command.AnalyzeListingCommand(facade,text.toString()));
        output.println(analysis.draft().describe()); output.println(ResultFormatter.format(analysis.quality()));
        output.println("Only explicit canonical units can prefill CAD/km/L/100km fields. Correct uncertain or unsupported units yourself.");
        var values = analysis.draft().formValues();
        while (input.hasNextLine()) {
            output.println("Choose confirm, edit, or cancel (saved vehicle is unchanged until final yes):");
            String action = input.nextLine().strip().toLowerCase(Locale.ROOT);
            if (action.equals("cancel")) return;
            if (action.equals("edit")) {
                for (var field : PreparationForm.FIELDS) {
                    if (!field.section().equals("Vehicle")) continue;
                    output.println(field.label() + " [draft=" + values.get(field.key()) + "] (:keep retains, blank clears, :cancel cancels):");
                    if (!input.hasNextLine()) return;
                    String value = input.nextLine(); if (value.equals(":cancel")) return;
                    if (!value.equals(":keep")) values.put(field.key(),value);
                }
            } else if (!action.equals("confirm")) continue;
            try {
                var candidate = PreparationForm.vehicle(facade,values);
                output.println(candidate); output.println(ResultFormatter.format(facade.reviewListing(analysis,candidate)));
                output.println("Save these reviewed claims and replace the current vehicle, if any? Type yes; anything else keeps the draft unsaved.");
                if (!input.hasNextLine()) return;
                if (input.nextLine().strip().equalsIgnoreCase("yes")) {
                    facade.confirmListing(analysis,candidate,true); output.println("User-confirmed listing saved; buyer preferences preserved."); return;
                }
            } catch (IllegalArgumentException | IllegalStateException e) { output.println("ERROR: " + e.getMessage()); }
        }
    }

    private String clarificationCycle;
    private void clarification(String raw){
        String[] p=raw.split("\\s+",3);String action=p[0].toLowerCase(Locale.ROOT);
        ca.yorku.eecs3311.usedcar.result.ClarificationState state;
        switch(action){
            case "clarify","clarify-next" -> {arity(p,1);state=facade.startClarification(action.equals("clarify-next"));}
            case "clarify-show" -> {arity(p,1);state=facade.currentClarification().orElseThrow(()->new IllegalStateException("Run clarify first."));}
            case "clarify-field" -> {arity(p,2);state=facade.revisitClarification(ca.yorku.eecs3311.usedcar.domain.ClarificationField.fromKey(p[1]));}
            case "clarify-answer" -> {arity(p,3);state=facade.submitClarification(clarificationCycle,ca.yorku.eecs3311.usedcar.domain.ClarificationField.fromKey(p[1]),p[2]);}
            case "clarify-skip","clarify-unknown" -> {arity(p,2);state=facade.skipClarification(clarificationCycle,ca.yorku.eecs3311.usedcar.domain.ClarificationField.fromKey(p[1]),action.equals("clarify-unknown"));}
            case "clarify-resolve" -> {arity(p,3);if(!p[2].equalsIgnoreCase("keep")&&!p[2].equalsIgnoreCase("accept"))throw new IllegalArgumentException("Use clarify-resolve <field> keep|accept.");
                state=facade.resolveClarification(clarificationCycle,ca.yorku.eecs3311.usedcar.domain.ClarificationField.fromKey(p[1]),p[2].equalsIgnoreCase("accept"));}
            case "clarify-continue" -> {
                if(p.length>2||p.length==2&&!p[1].equalsIgnoreCase("risk")&&!p[1].equalsIgnoreCase("checklist"))throw new IllegalArgumentException("Use clarify-continue [risk|checklist].");
                output.println(ResultFormatter.formatClarificationContinuation(facade.continueClarification(clarificationCycle,p.length==2&&p[1].equalsIgnoreCase("checklist"))));return;
            }
            default -> throw new IllegalArgumentException("Unknown clarification command. Type help.");
        }
        clarificationCycle=state.cycleId();output.println(ResultFormatter.format(state));
    }
    private static void arity(String[] parts,int expected){if(parts.length!=expected)throw new IllegalArgumentException("Incorrect clarification arguments. Type help for syntax.");}
    private void help() {
        output.println("Commands: checklist (generate; may request AI), checklist-show (cached), checklist-mark <id>, checklist-unmark <id>, comparison-set (save B), comparison-show (saved pair/assumptions), compare (one AI request when sufficient), evaluate (saved vehicle risk/evidence), analyze-listing (paste through :end, review/edit/confirm), prepare (enter/edit and calculate), demo (fictional input), check-conflicts (second structured record), knowledge (saved vehicle), profile-set, profile-show, show, status, llm-test (one live request), help, quit.");
        output.println("F11: clarify (start/reopen cached; may request AI), clarify-next (next unasked gaps), clarify-show, clarify-answer <field> <value>, clarify-skip <field>, clarify-unknown <field>, clarify-resolve <field> keep|accept, clarify-field <field> (explicit revisit/correction), clarify-continue [risk|checklist] (real evaluation; may request AI). Finish each cycle before next/revisit/continue. Fields: accident, maintenance, mileage, price, transmission, consumption, fuelType, year, make, model, trim. Units: CAD, km, L/100 km; no commas or unit symbols in numeric answers.");
        output.println("strategy auto|balanced|budget|reliability selects and previews priority guidance; auto reselects from saved preferences, manual choices override it. strategy previews the retained mode.");
        output.println("prepare covers initial F01/F03/F04/F06/F07 slices. Local knowledge retrieval uses instructional CSV data. Listing analysis and risk assessment use explicit AI requests when evidence permits; comparison uses symmetric tools and explicit AI; checklist supports AI/fallback and local completion; clarification supports validated answers, memory and resume; report remains PLANNED.");
    }
}
