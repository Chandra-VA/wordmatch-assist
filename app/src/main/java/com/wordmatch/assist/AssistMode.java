package com.wordmatch.assist;

/** User-facing operating modes and the service capabilities each one enables. */
public enum AssistMode {
    LEARNING(false, false),
    AUTOMATIC(true, true);

    private final boolean autoClickEnabled;
    private final boolean freezeEnabled;

    AssistMode(boolean autoClickEnabled, boolean freezeEnabled) {
        this.autoClickEnabled = autoClickEnabled;
        this.freezeEnabled = freezeEnabled;
    }

    public boolean isAutoClickEnabled() {
        return autoClickEnabled;
    }

    public boolean isFreezeEnabled() {
        return freezeEnabled;
    }
}
