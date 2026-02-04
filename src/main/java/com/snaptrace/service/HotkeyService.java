package com.snaptrace.service;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

/**
 * Service for managing global hotkeys using JNativeHook.
 * Listens for Alt+Shift+S (capture) and Alt+Shift+D (dashboard).
 */
public class HotkeyService implements NativeKeyListener {
    
    private static final Logger logger = LoggerFactory.getLogger(HotkeyService.class);
    
    private HotkeyCallback captureCallback;
    private HotkeyCallback dashboardCallback;
    
    private boolean altPressed = false;
    private boolean shiftPressed = false;
    
    private boolean isRegistered = false;
    
    @FunctionalInterface
    public interface HotkeyCallback {
        void onHotkeyPressed();
    }
    
    public HotkeyService() {
        // Disable JNativeHook's verbose logging
        java.util.logging.Logger jnhLogger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        jnhLogger.setLevel(Level.OFF);
        jnhLogger.setUseParentHandlers(false);
    }
    
    /**
     * Registers the global hotkey listener.
     * @throws RuntimeException if registration fails
     */
    public void register() {
        if (isRegistered) {
            logger.warn("Hotkey service already registered");
            return;
        }
        
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            isRegistered = true;
            logger.info("Global hotkey listener registered successfully");
            logger.info("Capture hotkey: Alt+Shift+S");
            logger.info("Dashboard hotkey: Alt+Shift+D");
        } catch (NativeHookException e) {
            logger.error("Failed to register native hook. You may need to grant accessibility permissions.", e);
            throw new RuntimeException("Failed to register global hotkeys. Please ensure the application has the required permissions.", e);
        }
    }
    
    /**
     * Unregisters the global hotkey listener.
     */
    public void unregister() {
        if (!isRegistered) {
            return;
        }
        
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            isRegistered = false;
            logger.info("Global hotkey listener unregistered");
        } catch (NativeHookException e) {
            logger.error("Failed to unregister native hook", e);
        }
    }
    
    /**
     * Sets the callback for the capture hotkey (Alt+Shift+S).
     * @param callback The callback to invoke
     */
    public void setCaptureCallback(HotkeyCallback callback) {
        this.captureCallback = callback;
    }
    
    /**
     * Sets the callback for the dashboard hotkey (Alt+Shift+D).
     * @param callback The callback to invoke
     */
    public void setDashboardCallback(HotkeyCallback callback) {
        this.dashboardCallback = callback;
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Track modifier keys
        if (keyCode == NativeKeyEvent.VC_ALT) {
            altPressed = true;
        } else if (keyCode == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = true;
        }
        
        // Check for hotkey combinations
        if (altPressed && shiftPressed) {
            if (keyCode == NativeKeyEvent.VC_S) {
                logger.debug("Capture hotkey triggered (Alt+Shift+S)");
                if (captureCallback != null) {
                    captureCallback.onHotkeyPressed();
                }
            } else if (keyCode == NativeKeyEvent.VC_D) {
                logger.debug("Dashboard hotkey triggered (Alt+Shift+D)");
                if (dashboardCallback != null) {
                    dashboardCallback.onHotkeyPressed();
                }
            }
        }
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Track modifier key release
        if (keyCode == NativeKeyEvent.VC_ALT) {
            altPressed = false;
        } else if (keyCode == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = false;
        }
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used
    }
    
    public boolean isRegistered() {
        return isRegistered;
    }
}
