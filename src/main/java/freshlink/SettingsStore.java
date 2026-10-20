package freshlink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** User-local preferences backed by java.util.Properties. */
public final class SettingsStore {
    private final Path file = Paths.get(System.getProperty("user.home"), ".freshlink", "settings.properties");
    private final Properties values = new Properties();
    public SettingsStore() { if(Files.exists(file))try(InputStream in=Files.newInputStream(file)){values.load(in);}catch(IOException ex){System.err.println("FreshLink: could not load settings: "+ex.getMessage());} }
    public String get(String key,String fallback){return values.getProperty(key,fallback);}
    public boolean getBoolean(String key,boolean fallback){return Boolean.parseBoolean(values.getProperty(key,Boolean.toString(fallback)));}
    public void put(String key,String value){values.setProperty(key,value);save();}
    public void put(String key,boolean value){put(key,Boolean.toString(value));}
    private void save(){try{Files.createDirectories(file.getParent());Path temp=file.resolveSibling("settings.tmp");try(OutputStream out=Files.newOutputStream(temp)){values.store(out,"FreshLink local preferences");}try{Files.move(temp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(temp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(IOException ex){System.err.println("FreshLink: could not save settings: "+ex.getMessage());}}
}
