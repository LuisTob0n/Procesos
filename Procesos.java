package procesos;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.StdCallLibrary;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Procesos {
    
    private static JTextArea textArea;
    
    public Procesos(JTextArea textArea2){
        
        this.textArea = textArea2;
        
    }

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = (User32) Native.load("user32", User32.class);
        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        Pointer GetWindowLongPtr(HWND hWnd, int nIndex);
    }
    
    private static Map<String, LocalDateTime> startTimes = new HashMap<>();
    private static Map<String, Long> appUsageTimes = new HashMap<>();
    private static boolean firstRun = true;
    
    public static void main(String[] args) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(Procesos::checkApps, 0, 1, TimeUnit.SECONDS);
    }

    public static void checkApps() {
        final User32 user32 = User32.INSTANCE;
        user32.EnumWindows(new WNDENUMPROC() {
            int count = 0;
            public boolean callback(HWND hWnd, Pointer arg1) {
                byte[] windowText = new byte[512];
                user32.GetWindowTextA(hWnd, windowText, 512);
                String wText = Native.toString(windowText);

                if (!wText.isEmpty()) {
                    if (!startTimes.containsKey(wText)) {
                        if (firstRun && wText.contains("Word")) {
                            System.out.println("Iniciando seguimiento para: " + wText);
                        }
                        startApp(wText);
                    }
                }
                return true;
            }
        }, null);

        // Crear una copia de startTimes.keySet()
        Set<String> appsToCheck = new HashSet<>(startTimes.keySet());

        for (String app : appsToCheck) {
            if (!appIsOpen(app)) {
                //System.out.println("Aplicación cerrada detectada: " + app);
                stopApp(app);
            }
        }

        firstRun = false;
    }

    public static void startApp(String appName) {
        if(appName.contains("Word")){
            System.out.println("Iniciando aplicación: " + appName);
            startTimes.put(appName, LocalDateTime.now());
        }else{
            
        }
    }

    public static void stopApp(String appName) {
        //System.out.println("Deteniendo aplicación: " + appName);
        LocalDateTime startTime = startTimes.get(appName);
        if (startTime != null && appName.contains("Word") ) {
            long totalMinutes = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            long seconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) % 60;
            appUsageTimes.put(appName, totalMinutes);
            //System.out.println("\nHas usado " + appName + " durante " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos.");
            String message = "\nHas usado " + appName + " durante " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos.";
            SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textArea.append(message + "\n");
            }
        });
            textArea.append(message + "\n");
            startTimes.remove(appName);
        } else {
            System.out.println("No se encontró el tiempo de inicio para " + appName);
        }
    }

    public static boolean appIsOpen(String appName) {
        final User32 user32 = User32.INSTANCE;
        final boolean[] appFound = {false};
        user32.EnumWindows(new WNDENUMPROC() {
            int count = 0;
            public boolean callback(HWND hWnd, Pointer arg1) {
                byte[] windowText = new byte[512];
                user32.GetWindowTextA(hWnd, windowText, 512);
                String wText = Native.toString(windowText);

                if (wText.startsWith(appName)) {
                    appFound[0] = true;
                    return false;
                }
                return true;
            }
        }, null);
        return appFound[0];
    }
}
