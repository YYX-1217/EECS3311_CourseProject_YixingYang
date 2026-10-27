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
    public String readCommand() { return input.hasNextLine() ? input.nextLine().strip().toLowerCase(Locale.ROOT) : "quit"; }

    public void demo() {
        output.println("EXAMPLE INPUT: fictional vehicle and explicit sample assumptions, not real market data.");
        displayResult(submit(PreparationForm.command(facade, PreparationForm.example())));
    }

    public void run() {
        output.println("AI Used-Car Evaluation Assistant - Stage 2 foundation");
        help();
        while (true) {
            output.print("usedcar> "); output.flush();
            String command = readCommand();
            try {
                switch (command) {
                    case "quit", "exit" -> { return; }
                    case "help", "" -> help();
                    case "demo" -> demo();
                    case "prepare" -> enterPreparation();
                    case "show" -> {
                        output.println(facade.currentVehicle().map(Object::toString).orElse("No vehicle saved yet."));
                        output.println(facade.preferences());
                    }
                    case "status" -> output.println(facade.llmStatus());
                    case "evaluate", "analyze-listing", "compare", "checklist", "report", "knowledge", "clarify" ->
                            output.println("PLANNED: this feature is not implemented. " + facade.llmStatus());
                    default -> output.println("Unknown command. Type help.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                output.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void enterPreparation() {
        output.println("Enter a vehicle, buyer limits and cost assumptions. Blank optional fields stay unknown/neutral.");
        output.println("This replaces the current in-memory vehicle after successful calculation. Type :cancel to cancel.");
        var values = new LinkedHashMap<String, String>();
        for (var field : PreparationForm.FIELDS) {
            output.print(field.label() + ": "); output.flush();
            if (!input.hasNextLine()) { output.println("Cancelled at end of input; previous session preserved."); return; }
            String value = input.nextLine();
            if (value.equalsIgnoreCase(":cancel")) { output.println("Cancelled; previous session preserved."); return; }
            values.put(field.key(), value);
        }
        displayResult(submit(PreparationForm.command(facade, values)));
    }

    private void help() {
        output.println("Commands: prepare (enter/edit and calculate), demo (fictional input), show, status, help, quit.");
        output.println("prepare covers initial F01/F03/F04/F06/F07 slices. AI, knowledge retrieval and comparison are PLANNED.");
    }
}
