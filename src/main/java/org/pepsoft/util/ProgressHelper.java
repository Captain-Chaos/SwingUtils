package org.pepsoft.util;

import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

import static java.awt.Taskbar.Feature.PROGRESS_STATE_WINDOW;
import static java.awt.Taskbar.Feature.PROGRESS_VALUE_WINDOW;

/**
 * Created by Pepijn on 27-11-2016.
 */
class ProgressHelper {
    static ProgressHelper getInstance() {
        return INSTANCE;
    }

    ProgressHelper() {
        enabled = Taskbar.isTaskbarSupported()
                && Taskbar.getTaskbar().isSupported(PROGRESS_VALUE_WINDOW)
                && Taskbar.getTaskbar().isSupported(PROGRESS_STATE_WINDOW);
    }

    void setProgress(Window window, int percentage) {
        if (! enabled) {
            return;
        }
        if (errorStates.getOrDefault(window, false)) {
            return;
        }
        Taskbar.getTaskbar().setWindowProgressValue(window, percentage);
    }

    void setProgressDone(Window window) {
        if (! enabled) {
            return;
        }
        Taskbar.getTaskbar().setWindowProgressState(window, Taskbar.State.OFF);
        errorStates.remove(window);
    }

    void setProgressError(Window window) {
        if (! enabled) {
            return;
        }
        if (errorStates.getOrDefault(window, false)) {
            return;
        } else {
            errorStates.put(window, true);
        }
        Taskbar.getTaskbar().setWindowProgressState(window, Taskbar.State.ERROR);
    }

    private final boolean enabled;
    private final Map<Window, Boolean> errorStates = new WeakHashMap<>();

    private static final ProgressHelper INSTANCE = new ProgressHelper();
}