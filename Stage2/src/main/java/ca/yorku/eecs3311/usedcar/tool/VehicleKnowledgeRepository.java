package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.result.VehicleKnowledge;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Restricted UTF-8 CSV: eight columns, no quoting/multiline fields; lists use '|'. */
public final class VehicleKnowledgeRepository {
    private static final String HEADER = "make,model,yearFrom,yearTo,summary,commonIssues,inspectionNotes,sourceLabel";
    private final Path dataFilePath;
    private List<Entry> entries;
    private record Entry(String make, String model, int from, int to, VehicleKnowledge knowledge) { }

    /** Uses the resource packaged in the application JAR, independent of the working directory. */
    public VehicleKnowledgeRepository() { dataFilePath = null; }
    public VehicleKnowledgeRepository(Path dataFilePath) { this.dataFilePath = Objects.requireNonNull(dataFilePath); }

    public void load() {
        String source = dataFilePath == null ? "classpath:/vehicle-knowledge.csv" : dataFilePath.toString();
        // Invalidate old data before reload so a failed load cannot silently serve stale records.
        entries = null;
        try (InputStream stream = dataFilePath == null
                ? VehicleKnowledgeRepository.class.getResourceAsStream("/vehicle-knowledge.csv")
                : Files.newInputStream(dataFilePath)) {
            if (stream == null) throw new IOException("Data file is missing");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                if (!HEADER.equals(reader.readLine())) throw malformed(1, "Expected CSV header: " + HEADER);
                var loaded = new ArrayList<Entry>();
                String line; int number = 1;
                while ((line = reader.readLine()) != null) {
                    number++;
                    if (line.isBlank()) continue;
                    if (line.indexOf('"') >= 0) throw malformed(number, "Quoted fields are not supported");
                    String[] cells = line.split(",", -1);
                    if (cells.length != 8) throw malformed(number, "Expected exactly eight columns; embedded commas are not supported");
                    for (int i = 0; i < cells.length; i++) cells[i] = cells[i].strip();
                    for (int i : new int[]{0, 1, 2, 3, 4, 7})
                        if (cells[i].isEmpty()) throw malformed(number, "Required column is blank: " + (i + 1));
                    int from, to;
                    try { from = Integer.parseInt(cells[2]); to = Integer.parseInt(cells[3]); }
                    catch (NumberFormatException e) { throw malformed(number, "Years must be integers"); }
                    if (from < 1886 || to > 9999 || from > to) throw malformed(number, "Invalid inclusive year range");
                    var entry = new Entry(normalize(cells[0]), normalize(cells[1]), from, to,
                            new VehicleKnowledge(cells[4], items(cells[5], number), items(cells[6], number), cells[7]));
                    for (var previous : loaded)
                        if (previous.make().equals(entry.make()) && previous.model().equals(entry.model())
                                && from <= previous.to() && to >= previous.from())
                            throw malformed(number, "Overlapping year ranges for the same make/model");
                    loaded.add(entry);
                }
                entries = List.copyOf(loaded);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot load local vehicle knowledge from " + source + ": " + e.getMessage(), e);
        }
    }

    public VehicleKnowledge findByVehicle(String make, String model, int year) {
        String normalizedMake = normalize(make), normalizedModel = normalize(model);
        if (entries == null) load();
        return entries.stream().filter(e -> e.make().equals(normalizedMake) && e.model().equals(normalizedModel)
                && year >= e.from() && year <= e.to()).map(Entry::knowledge).findFirst().orElseGet(VehicleKnowledge::noData);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Make and model are required for knowledge lookup");
        return value.strip().toLowerCase(Locale.ROOT);
    }
    private static List<String> items(String value, int line) {
        if (value.isEmpty()) return List.of();
        var items = Arrays.stream(value.split("\\|", -1)).map(String::strip).toList();
        if (items.stream().anyMatch(String::isEmpty)) throw malformed(line, "Empty list item");
        return items;
    }
    private static IllegalArgumentException malformed(int line, String reason) {
        return new IllegalArgumentException("Malformed CSV at line " + line + ": " + reason);
    }
}
