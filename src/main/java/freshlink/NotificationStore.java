package freshlink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** CSV persistence for local user notifications. */
public final class NotificationStore {
    private final Path file=Paths.get(System.getProperty("user.home"),".freshlink","notifications.csv");
    public List<AppNotification> load() throws IOException {List<AppNotification> out=new ArrayList<>();if(!Files.exists(file))return out;int n=0;
        for(List<String> f:CsvUtil.parseRows(Files.readString(file,StandardCharsets.UTF_8))){n++;try{if(f.size()==5&&!f.get(0).equals("id"))out.add(new AppNotification(f.get(0),f.get(1),f.get(2),LocalDateTime.parse(f.get(3)),Boolean.parseBoolean(f.get(4))));}catch(RuntimeException ex){System.err.println("FreshLink: skipped damaged notification row "+n);}}return out;}
    public void save(List<AppNotification> entries)throws IOException{Files.createDirectories(file.getParent());List<String> rows=new ArrayList<>();rows.add(CsvUtil.row("id","recipient","message","createdAt","read"));for(AppNotification n:entries)rows.add(CsvUtil.row(n.id(),n.recipient(),n.message(),n.createdAt().toString(),Boolean.toString(n.read())));Path temp=file.resolveSibling("notifications.tmp");Files.write(temp,rows,StandardCharsets.UTF_8);try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
}
