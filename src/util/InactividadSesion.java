package util;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import javax.swing.Timer;

/** Cierra la sesión tras un tiempo sin actividad (mouse o teclado). */
public class InactividadSesion {

    public static final int MINUTOS = 10;

    private final Timer timer;
    private final AWTEventListener listener;

    public InactividadSesion(Runnable alExpirar) {
        timer = new Timer(MINUTOS * 60 * 1000, e -> {
            detener();
            alExpirar.run();
        });
        timer.setRepeats(false);

        listener = event -> timer.restart();
    }

    public void iniciar() {
        Toolkit.getDefaultToolkit().addAWTEventListener(listener,
                AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK
                | AWTEvent.KEY_EVENT_MASK);
        timer.start();
    }

    public void detener() {
        timer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
    }
}
