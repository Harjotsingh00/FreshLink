package freshlink;

import java.util.*;

/** Minimal RFC 4180 style CSV row encoder and parser. */
public final class CsvUtil {
    private CsvUtil() { }
    public static String row(String... values) {
        return String.join(",", Arrays.stream(values).map(CsvUtil::escape).toList());
    }
    private static String escape(String value) {
        String s = value == null ? "" : value;
        return s.matches(".*[,\"\\r\\n].*") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted && c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cell.append('"'); i++; }
            else if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { fields.add(cell.toString()); cell.setLength(0); }
            else cell.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        fields.add(cell.toString()); return fields;
    }
    public static List<List<String>> parseRows(String text) {
        List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted && c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
            else if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { row.add(cell.toString()); cell.setLength(0); }
            else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0); rows.add(row); row = new ArrayList<>();
            } else cell.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }
}
