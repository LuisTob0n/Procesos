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

public class Procesos {

  public static JTextArea txtArea2;

  // Interface definition
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
    user32.EnumWindows(new WinUser.WNDENUMPROC() {
      int count = 0;
      public boolean callback(HWND hWnd, Pointer arg1) {
        byte[] windowText = new byte[512];
        user32.GetWindowTextA(hWnd, windowText, 512);
        String wText = Native.toString(windowText);

        if (!wText.isEmpty()) {
          if (!startTimes.containsKey(wText)) {
            if (firstRun) {
              txtArea2.append("Iniciando seguimiento para: " + wText + "\n");
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
        txtArea2.append("Aplicación cerrada detectada: " + app + "\n");
        stopApp(app);
      }
    }

    firstRun = false;
  }

  public static void startApp(String appName) {
    String message = "Iniciando aplicación: " + appName + "\n";
    txtArea2.append(message);
    startTimes.put(appName, LocalDateTime.now());
  }

  public static void stopApp(String appName) {
    String message = "Deteniendo aplicación: " + appName + "\n";
    txtArea2.append(message);
    LocalDateTime startTime = startTimes.get(appName);
    if (startTime != null) {
      long totalMinutes = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
      long hours = totalMinutes / 60;
      long minutes = totalMinutes % 60;
      long seconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) % 60;
      appUsageTimes.put(appName, totalMinutes);
      txtArea2.append("Has usado " + appName + " durante " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos.\n");
      startTimes.remove(appName);
    } else {
      txtArea2.append("No se encontró el tiempo de inicio para " + appName + "\n");
    }
  }

  public static boolean appIsOpen(String appName) {
        final User32 user32 = User32.INSTANCE;
        final boolean[] appFound = {false};
        user32.EnumWindows(new WinUser.WNDENUMPROC() {
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
