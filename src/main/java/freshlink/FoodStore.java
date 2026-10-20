package freshlink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Local CSV repository with atomic replacement and rolling backups. */
public final class FoodStore {
    private final Path file = Paths.get(System.getProperty("user.home"), ".freshlink", "listings.csv");
    public boolean hasSavedInventory() { return Files.exists(file); }

    public List<FoodItem> load() throws IOException {
        List<FoodItem> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        List<List<String>> rows;
        try { rows = CsvUtil.parseRows(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (RuntimeException ex) { System.err.println("FreshLink: inventory CSV has an incomplete quoted row; readable rows will be checked individually."); rows = new ArrayList<>(); for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) try { rows.add(CsvUtil.parse(line)); } catch (RuntimeException ignored) { } }
        for (int n = 0; n < rows.size(); n++) {
            List<String> f = rows.get(n); if (f.isEmpty() || f.stream().allMatch(String::isBlank)) continue;
            try {
                if (f.size() == 10) { // legacy format
                    result.add(new FoodItem(dec(f.get(0)), dec(f.get(1)), dec(f.get(2)), Integer.parseInt(f.get(3)),
                            dec(f.get(4)), LocalDate.parse(dec(f.get(5))), dec(f.get(6)), dec(f.get(7)), dec(f.get(8)), dec(f.get(9))));
                } else if (f.size() == 11 && f.get(0).equals("id")) continue;
                else if (f.size() == 11) {
                    result.add(new FoodItem(f.get(0), f.get(1), f.get(2), Integer.parseInt(f.get(3)), f.get(4),
                            LocalDate.parse(f.get(5)), f.get(6), f.get(7), f.get(8), f.get(9), f.get(10)));
                } else if (f.size() == 15 && f.get(0).equals("id")) continue;
                else if (f.size() == 15) {
                    result.add(new FoodItem(f.get(0),f.get(1),f.get(2),Integer.parseInt(f.get(3)),f.get(4),LocalDate.parse(f.get(5)),f.get(6),f.get(7),f.get(8),f.get(9),f.get(10),f.get(11),
                            f.get(12).isBlank()?null:java.time.LocalTime.parse(f.get(12)),f.get(13).isBlank()?null:java.time.LocalTime.parse(f.get(13)),f.get(14)));
                } else System.err.println("FreshLink: skipped malformed listing row " + (n + 1));
            } catch (RuntimeException ex) { System.err.println("FreshLink: skipped damaged listing row " + (n + 1)); }
        }
        return result;
    }

    public void save(List<FoodItem> items) throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.exists(file) && Files.size(file) > 0) backup();
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtil.row("id", "name", "category", "quantity", "unit", "expiry", "donor", "location", "notes", "collectedBy", "reservedBy", "contactNumber", "pickupFrom", "pickupUntil", "priority"));
        for (FoodItem i : items) lines.add(CsvUtil.row(i.getId(), i.getName(), i.getCategory(), Integer.toString(i.getQuantity()),
                i.getUnit(), i.getExpiryDate().toString(), i.getDonor(), i.getLocation(), i.getNotes(), i.getClaimedBy(), i.getReservedBy(),i.getContactNumber(),
                i.getPickupFrom()==null?"":i.getPickupFrom().toString(),i.getPickupUntil()==null?"":i.getPickupUntil().toString(),i.getPriority()));
        Path temp = file.resolveSibling("listings.tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    public List<Path> backups() throws IOException {
        Path dir = file.getParent().resolve("backups"); if (!Files.isDirectory(dir)) return List.of();
        try (var s = Files.list(dir)) { return s.filter(p -> p.getFileName().toString().endsWith(".csv")).sorted(Comparator.reverseOrder()).toList(); }
    }
    public void restore(Path backup) throws IOException {
        if (!backup.normalize().startsWith(file.getParent().resolve("backups").normalize())) throw new IOException("Invalid backup path");
        List<String> content = Files.readAllLines(backup, StandardCharsets.UTF_8);
        Path temp = file.resolveSibling("listings.restore.tmp"); Files.write(temp, content, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
    private void backup() throws IOException {
        Path dir = file.getParent().resolve("backups"); Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        Files.copy(file, dir.resolve("listings-" + stamp + ".csv"), StandardCopyOption.REPLACE_EXISTING);
        List<Path> all = backups(); for (int i = 10; i < all.size(); i++) Files.deleteIfExists(all.get(i));
    }
    private static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8); }
}
