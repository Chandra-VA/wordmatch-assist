package com.wordmatch.assist.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wordmatch.assist.R;

/** Compact accessibility-overlay control for switching modes without leaving Duolingo. */
@SuppressLint("ViewConstructor")
public final class ModeControlView extends LinearLayout {
    public interface Listener {
        void onLearningModeSelected();

        void onAutomaticModeSelected();

        void onTakeoverSelected();

        void onCloseSelected();
    }

    private static final int COLOR_LEARNING = 0xFF1CB0F6;
    private static final int COLOR_AUTOMATIC = 0xFF58CC02;
    private static final int COLOR_TAKEOVER = 0xFFFF9600;
    private static final int COLOR_CLOSE = 0xFFFF4B4B;
    private static final int COLOR_INACTIVE = 0xFFF0F4F7;
    private static final int COLOR_INK = 0xFF243041;

    private final TextView bubble;
    private final LinearLayout actionPanel;
    private final LinearLayout takeoverPanel;
    private final TextView takeoverReason;
    private final TextView learningButton;
    private final TextView automaticButton;
    private boolean automatic;
    private boolean takeoverRequired;

    public ModeControlView(Context context, Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        setClipChildren(false);
        setClipToPadding(false);

        bubble = createButton(context, "");
        LayoutParams bubbleParams = new LayoutParams(dp(54), dp(54));
        bubble.setLayoutParams(bubbleParams);
        bubble.setTextSize(17f);
        bubble.setElevation(dp(8));
        bubble.setOnClickListener(ignored -> setExpanded(!isExpanded()));
        addView(bubble);

        actionPanel = new LinearLayout(context);
        actionPanel.setOrientation(HORIZONTAL);
        actionPanel.setGravity(Gravity.CENTER_VERTICAL);
        actionPanel.setPadding(dp(5), dp(5), dp(5), dp(5));
        actionPanel.setBackground(roundRect(0xF7FFFFFF, 15f, 0x22000000, 1f));
        actionPanel.setElevation(dp(8));
        LayoutParams panelParams = new LayoutParams(dp(206), dp(48));
        panelParams.topMargin = dp(5);
        actionPanel.setLayoutParams(panelParams);

        learningButton = createButton(context, context.getString(R.string.floating_learning));
        automaticButton = createButton(context, context.getString(R.string.floating_automatic));
        TextView closeButton = createButton(context, context.getString(R.string.floating_close));

        addActionButton(learningButton, 1f, 0);
        addActionButton(automaticButton, 1f, dp(4));
        addActionButton(closeButton, 0.82f, dp(4));
        learningButton.setOnClickListener(ignored -> {
            listener.onLearningModeSelected();
            setExpanded(false);
        });
        automaticButton.setOnClickListener(ignored -> {
            listener.onAutomaticModeSelected();
            setExpanded(false);
        });
        closeButton.setOnClickListener(ignored -> listener.onCloseSelected());
        closeButton.setTextColor(Color.WHITE);
        closeButton.setBackground(roundRect(COLOR_CLOSE, 11f, 0, 0f));

        actionPanel.setVisibility(GONE);
        addView(actionPanel);

        takeoverPanel = new LinearLayout(context);
        takeoverPanel.setOrientation(VERTICAL);
        takeoverPanel.setPadding(dp(12), dp(10), dp(12), dp(10));
        takeoverPanel.setBackground(roundRect(0xFAFFFFFF, 15f, COLOR_TAKEOVER, 2f));
        takeoverPanel.setElevation(dp(9));
        LayoutParams takeoverParams = new LayoutParams(dp(258), LayoutParams.WRAP_CONTENT);
        takeoverParams.topMargin = dp(5);
        takeoverPanel.setLayoutParams(takeoverParams);

        TextView takeoverTitle = new TextView(context);
        takeoverTitle.setText(R.string.floating_takeover_title);
        takeoverTitle.setTextColor(COLOR_INK);
        takeoverTitle.setTextSize(14f);
        takeoverTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        takeoverPanel.addView(takeoverTitle, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        takeoverReason = new TextView(context);
        takeoverReason.setTextColor(0xFF657286);
        takeoverReason.setTextSize(11f);
        takeoverReason.setMaxLines(2);
        LayoutParams reasonParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        reasonParams.topMargin = dp(3);
        takeoverPanel.addView(takeoverReason, reasonParams);

        LinearLayout takeoverActions = new LinearLayout(context);
        takeoverActions.setOrientation(HORIZONTAL);
        LayoutParams takeoverActionsParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(40)
        );
        takeoverActionsParams.topMargin = dp(8);
        takeoverPanel.addView(takeoverActions, takeoverActionsParams);

        TextView takeoverButton = createButton(
                context,
                context.getString(R.string.floating_takeover_button)
        );
        takeoverButton.setTextColor(Color.WHITE);
        takeoverButton.setBackground(roundRect(COLOR_TAKEOVER, 11f, 0, 0f));
        takeoverButton.setOnClickListener(ignored -> listener.onTakeoverSelected());
        takeoverActions.addView(takeoverButton, new LayoutParams(0, dp(40), 1.5f));

        TextView takeoverCloseButton = createButton(
                context,
                context.getString(R.string.floating_close)
        );
        takeoverCloseButton.setTextColor(Color.WHITE);
        takeoverCloseButton.setBackground(roundRect(COLOR_CLOSE, 11f, 0, 0f));
        takeoverCloseButton.setOnClickListener(ignored -> listener.onCloseSelected());
        LayoutParams takeoverCloseParams = new LayoutParams(0, dp(40), 1f);
        takeoverCloseParams.leftMargin = dp(6);
        takeoverActions.addView(takeoverCloseButton, takeoverCloseParams);

        takeoverPanel.setVisibility(GONE);
        addView(takeoverPanel);
        setAutomatic(false);
    }

    public View getDragHandle() {
        return bubble;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
        takeoverRequired = false;
        takeoverPanel.setVisibility(GONE);
        actionPanel.setVisibility(GONE);
        bubble.setText(automatic
                ? getContext().getString(R.string.floating_automatic_short)
                : getContext().getString(R.string.floating_learning_short));
        bubble.setBackground(roundRect(
                automatic ? COLOR_AUTOMATIC : COLOR_LEARNING,
                27f,
                Color.WHITE,
                3f
        ));
        bubble.setTextColor(Color.WHITE);
        bubble.setContentDescription(getContext().getString(
                automatic
                        ? R.string.floating_automatic_description
                        : R.string.floating_learning_description
        ));
        updateActionStyles();
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public boolean isExpanded() {
        return takeoverRequired
                ? takeoverPanel.getVisibility() == VISIBLE
                : actionPanel.getVisibility() == VISIBLE;
    }

    public void setExpanded(boolean expanded) {
        if (takeoverRequired) {
            takeoverPanel.setVisibility(expanded ? VISIBLE : GONE);
            actionPanel.setVisibility(GONE);
        } else {
            actionPanel.setVisibility(expanded ? VISIBLE : GONE);
            takeoverPanel.setVisibility(GONE);
        }
        bubble.setContentDescription(getContext().getString(
                expanded
                        ? R.string.floating_collapse_description
                        : takeoverRequired
                                ? R.string.floating_takeover_description
                                : (automatic
                                        ? R.string.floating_automatic_description
                                        : R.string.floating_learning_description)
        ));
    }

    public void showTakeoverPrompt(String reason) {
        takeoverRequired = true;
        actionPanel.setVisibility(GONE);
        takeoverReason.setText(getContext().getString(
                R.string.floating_takeover_reason,
                reason == null || reason.trim().isEmpty()
                        ? getContext().getString(R.string.floating_takeover_unknown_reason)
                        : reason.trim()
        ));
        bubble.setText(R.string.floating_takeover_short);
        bubble.setTextColor(Color.WHITE);
        bubble.setBackground(roundRect(COLOR_TAKEOVER, 27f, Color.WHITE, 3f));
        bubble.setContentDescription(
                getContext().getString(R.string.floating_takeover_description)
        );
        takeoverPanel.setVisibility(VISIBLE);
    }

    private void addActionButton(TextView button, float weight, int leftMargin) {
        LayoutParams params = new LayoutParams(0, dp(38), weight);
        params.leftMargin = leftMargin;
        actionPanel.addView(button, params);
    }

    private void updateActionStyles() {
        styleModeButton(learningButton, !automatic, COLOR_LEARNING);
        styleModeButton(automaticButton, automatic, COLOR_AUTOMATIC);
    }

    private void styleModeButton(TextView button, boolean active, int activeColor) {
        button.setTextColor(active ? Color.WHITE : COLOR_INK);
        button.setBackground(roundRect(
                active ? activeColor : COLOR_INACTIVE,
                11f,
                0,
                0f
        ));
    }

    private TextView createButton(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(13f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(5), 0, dp(5), 0);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private GradientDrawable roundRect(
            int color,
            float radiusDp,
            int strokeColor,
            float strokeDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0f) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
