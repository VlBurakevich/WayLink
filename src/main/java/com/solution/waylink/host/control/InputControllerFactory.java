package com.solution.waylink.host.control;

import com.solution.waylink.host.control.native_impl.MacCoreGraphicsController;
import com.solution.waylink.host.control.native_impl.NativeInputController;
import com.solution.waylink.host.control.native_impl.WaylandInputController;
import com.solution.waylink.host.control.native_impl.WindowsSendInputController;
import com.solution.waylink.host.control.native_impl.X11InputController;
import com.sun.jna.Platform;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InputControllerFactory {
    private static final Logger logger = Logger.getLogger(InputControllerFactory.class.getName());

    private InputControllerFactory() {

    }

    public static NativeInputController createController() {
        if (Platform.isWindows()) {
            logger.log(Level.INFO, "Initializing Windows input controller");
            return new WindowsSendInputController();
        }

        if (Platform.isMac()) {
            logger.log(Level.INFO, "Initializing Mac input controller");
            return new MacCoreGraphicsController();
        }

        if (Platform.isLinux()) {
            String sessionType = System.getenv("XDG_SESSION_TYPE");
            logger.log(Level.INFO, "Linux environment detected type: {0}", sessionType);

            if ("wayland".equalsIgnoreCase(sessionType)) {
                try {
                    NativeInputController controller = new WaylandInputController();
                    logger.info("Wayland input controller successfully initialized.");
                    return controller;
                } catch (IllegalStateException e) {
                    logger.log(Level.WARNING, "Wayland controller initialization failed. Falling back to X11.", e);
                    return new X11InputController();
                }
            }

            logger.info("Initializing X11 input controller.");
            return new X11InputController();
        }

        throw new UnsupportedOperationException("Unsupported platform");
    }
}
