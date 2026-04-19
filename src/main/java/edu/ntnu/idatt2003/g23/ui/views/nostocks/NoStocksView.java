package edu.ntnu.idatt2003.g23.ui.views.nostocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Displayed when the player starts the game with no stocks loaded.
 *
 * <p>Shows a sequence of messages as a monologue. Each entry defines the text
 * and how long to display it before fading out. After the last message fades,
 * only the animated background and the back button remain.</p>
 */
public final class NoStocksView {

    /**
     * A single message in the monologue sequence.
     *
     * @param text        the text to display
     * @param displaySecs how many seconds to hold the message before fading out
     * @param gapSecs     how many seconds to pause after the message fades out;
     *                    {@code 0} means no fade-out — the next message swaps in-place (CUT)
     * @param appendBelow when {@code true} the text fades in as a new label underneath the
     *                    existing text rather than replacing it
     * @param holdOverlay when {@code true} and {@code gapSecs > 0}, only this message's label
     *                    fades out — the overlay stays visible for the next message
     */
    public record Message(String text, double displaySecs, double gapSecs, boolean appendBelow, boolean holdOverlay, boolean fadeIn) {
        /** Convenience — defaults: displaySecs=3, gapSecs=2, no append, holdOverlay=true, fadeIn=true. */
        public Message(String text) {
            this(text, 3, 2, false, true, true);
        }
        /** Convenience — no append, holdOverlay=true, default gapSecs=2, fadeIn=true. */
        public Message(String text, double displaySecs) {
            this(text, displaySecs, 2, false, true, true);
        }
        /** Convenience — no append, holdOverlay=true, explicit gap, fadeIn=true. */
        public Message(String text, double displaySecs, double gapSecs) {
            this(text, displaySecs, gapSecs, false, true, true);
        }
        /** Convenience — explicit append flag, holdOverlay=true, fadeIn=true. */
        public Message(String text, double displaySecs, double gapSecs, boolean appendBelow) {
            this(text, displaySecs, gapSecs, appendBelow, true, true);
        }
        /** Convenience — explicit append and holdOverlay, fadeIn=true. */
        public Message(String text, double displaySecs, double gapSecs, boolean appendBelow, boolean holdOverlay) {
            this(text, displaySecs, gapSecs, appendBelow, holdOverlay, true);
        }
    }

    private static final String MSG_SKIPPED = "I know you skipped all the stocks in the editor.";
    private static final String MSG_EMPTY   = "I know that file had nothing in it.\nDid you think I wouldn't notice?";

    private static final String APPLAUSE_SOUND = "/audio/sfx/Applause.mp3";

    /** The monologue played when the player has no stocks. */
    public static final List<Message> DEFAULT_MONOLOGUE = List.of(
            new Message("You didn't import any stocks, silly.", 6, 3, false, false),
            new Message("It's okay.", 2, 0, false, true, true), 
            new Message("I put a back button in the top left corner!", 4.25, 1.2), 
            new Message("You can go now.", 3.25, 4.8, false, false), 
            new Message("I mean it, the button is right there.", 4, 2.75),
            new Message("Here let me bring it closer"), //
            new Message("Okay, bye now!", 4, 10, false, false), 
            new Message("Still here?", 3, 1),
            new Message("I respect that.", 3, 1),
            new Message("Most people would have gone back by now.", 5, 3), 
            new Message("Not you though.", 3, 0), 
            new Message("You're different.", 3, 10, true, false),
            new Message("Let me bring the button back", 4, 3, false, false), //?
            new Message("", 5, 3.0, false, false), // swapped at runtime based on fromEditor
            new Message("That's fine.\nIt's not your fault.", 4, 1.5), 
            new Message("Let's just wait here for a little bit.", 3, 1, false, true),
            new Message("Let time pass.", 5, 8, false, false)
    );

    private NoStocksView() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds the no-stocks page.
     *
     * @param monologue  the sequence of messages to cycle through
     * @param fromEditor {@code true} if the player arrived via the CSV editor (skipped all rows),
     *                   {@code false} if the file was already empty before the editor was shown
     * @param onBack     called when the user presses Back
     * @return the page root
     */
    public static Parent build(List<Message> monologue, boolean fromEditor, Runnable onBack) {
        String contextMsg = fromEditor ? MSG_SKIPPED : MSG_EMPTY;

        // Remember the placeholder index BEFORE mapping so we can split on it
        int ctxIdx = -1;
        for (int i = 0; i < monologue.size(); i++) {
            if (monologue.get(i).text().isEmpty()) { ctxIdx = i; break; }
        }
        final int contextIdx = ctxIdx;

        final List<Message> msgs = monologue.stream()
                .map(m -> m.text().isEmpty() ? new Message(contextMsg, m.displaySecs(), m.gapSecs()) : m)
                .toList();

        // ── Page skeleton ──────────────────────────────────────────────────
        StackPane page = new StackPane();
        page.getStyleClass().add("home-page");

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> onBack.run());
        // Floating button layer — covers full screen, transparent to clicks in empty areas
        StackPane btnLayer = new StackPane(backBtn);
        btnLayer.setAlignment(Pos.TOP_LEFT);
        btnLayer.setPadding(new Insets(24, 0, 0, 32));
        btnLayer.setPickOnBounds(false);

        MediaPlayer applausePlayer = loadMediaPlayer(APPLAUSE_SOUND);
        if (applausePlayer != null) {
            applausePlayer.setVolume(0.05);
            applausePlayer.setOnReady(() -> applausePlayer.setVolume(0.05));
        }

        // labelsBox accumulates label lines; cleared whenever the overlay fades out
        VBox labelsBox = new VBox(12);
        labelsBox.setAlignment(Pos.CENTER);

        Runnable moveButtonToCenter = () -> {
            Bounds btnBounds = backBtn.localToScene(backBtn.getBoundsInLocal());
            double btnCenterX = btnBounds.getMinX() + btnBounds.getWidth() / 2;
            double sceneCenterX = backBtn.getScene().getWidth() / 2;
            Bounds textBounds = labelsBox.localToScene(labelsBox.getBoundsInLocal());
            double targetY = textBounds.getMaxY() + 32; // 32px below text
            double deltaY = targetY - btnBounds.getMinY();
            TranslateTransition slide = new TranslateTransition(Duration.millis(900), backBtn);
            slide.setToX(sceneCenterX - btnCenterX);
            slide.setToY(deltaY);
            slide.setInterpolator(Interpolator.EASE_BOTH);
            slide.play();
        };
        Runnable moveButtonBack = () -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(2));
            wait.setOnFinished(ev -> {
                TranslateTransition slide = new TranslateTransition(Duration.millis(900), backBtn);
                slide.setToX(0);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_BOTH);
                slide.setOnFinished(sv -> {
                    if (applausePlayer != null) {
                        applausePlayer.setVolume(0.05);
                        applausePlayer.seek(Duration.ZERO);
                        applausePlayer.play();
                    }
                });
                slide.play();
            });
            wait.play();
        };

        // Button bar — hidden by default, shown only during the interactive step
        Button yesBtn = new Button("Yes");
        Button noBtn  = new Button("No");
        yesBtn.getStyleClass().add("nostocks-btn");
        noBtn.getStyleClass().add("nostocks-btn");
        HBox buttonBar = new HBox(24, yesBtn, noBtn);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setVisible(false);

        VBox content = new VBox(24, labelsBox, buttonBar);
        content.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(content);
        overlay.getStyleClass().add("background-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);
        page.getChildren().addAll(overlay, btnLayer);

        // Stop and dispose the applause player when this page leaves the scene
        if (applausePlayer != null) {
            page.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    applausePlayer.stop();
                    applausePlayer.dispose();
                }
            });
        }

        // ── Simple path (no editor / no placeholder found) ──────────────────
        if (!fromEditor || contextIdx < 0) {
            page.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    playSequenceWithButtonHooks(msgs, labelsBox, overlay,
                            applausePlayer, moveButtonToCenter, moveButtonBack, null);
                }
            });
            return page;
        }

        // ── Interactive path ────────────────────────────────────────────────
        Message contextMessage = msgs.get(contextIdx);
        List<Message> phase1Msgs = msgs.subList(0, contextIdx);
        List<Message> phase2Msgs = msgs.subList(contextIdx + 1, msgs.size());

        SequentialTransition phase2    = buildPhaseSequence(phase2Msgs, labelsBox, overlay, null, true);
        SequentialTransition knewItSeq = buildPhaseSequence(
                List.of(new Message("I knew it, it's okay.", 4, 0)), labelsBox, overlay, null, true);
        knewItSeq.setOnFinished(e -> phase2.play());

        // Yes → hide buttons, play "I knew it" then phase 2
        Runnable onYes = () -> {
            buttonBar.setVisible(false);
            knewItSeq.play();
        };
        yesBtn.setOnAction(e -> onYes.run());

        // No → grow Yes button; at 10 clicks: "Don't lie to me" → clear → "Did you skip them all?" → yes/yes
        int[] noCount = {0};
        noBtn.setOnAction(e -> {
            noCount[0]++;
            if (noCount[0] >= 10) {
                yesBtn.setDisable(true);
                noBtn.setDisable(true);
                labelsBox.getChildren().clear();
                labelsBox.getChildren().add(makeLabel("Don't lie to me."));
                PauseTransition dontLieDelay = new PauseTransition(Duration.seconds(3));
                dontLieDelay.setOnFinished(ev -> {
                    buttonBar.setVisible(false);
                    labelsBox.getChildren().clear();
                    labelsBox.getChildren().add(makeLabel("Did you skip them all?"));
                    PauseTransition btnDelay = new PauseTransition(Duration.seconds(2));
                    btnDelay.setOnFinished(ev3 -> {
                            Button yes1 = new Button("Yes");
                            Button yes2 = new Button("Yes");
                            yes1.getStyleClass().add("nostocks-btn");
                            yes2.getStyleClass().add("nostocks-btn");
                            yes1.setOnAction(ev4 -> onYes.run());
                            yes2.setOnAction(ev4 -> onYes.run());
                            buttonBar.getChildren().setAll(yes1, yes2);
                            buttonBar.setVisible(true);
                        });
                        btnDelay.play();
                });
                dontLieDelay.play();
            } else {
                int newSize = 16 + noCount[0] * 2;
                yesBtn.setStyle("-fx-font-size: " + newSize + "px;");
            }
        });

        // Phase 1 ends → fade in context message, wait 3 s, then show buttons
        Runnable interactiveSetup = () -> {
            labelsBox.getChildren().clear();
            labelsBox.getChildren().add(makeLabel(contextMessage.text()));
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), overlay);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.setOnFinished(ev -> {
                PauseTransition readDelay = new PauseTransition(Duration.seconds(3));
                readDelay.setOnFinished(ev2 -> buttonBar.setVisible(true));
                readDelay.play();
            });
            fadeIn.play();
        };
        page.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                playSequenceWithButtonHooks(phase1Msgs, labelsBox, overlay,
                        applausePlayer, moveButtonToCenter, moveButtonBack, interactiveSetup);
            }
        });

        return page;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Label makeLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 28; -fx-text-fill: #b8cce8;");
        l.setTextAlignment(TextAlignment.CENTER);
        l.setAlignment(Pos.CENTER);
        l.setWrapText(true);
        l.setMaxWidth(700);
        return l;
    }

    private static SequentialTransition buildPhaseSequence(
            List<Message> messages, VBox labelsBox, StackPane overlay) {
        return buildPhaseSequence(messages, labelsBox, overlay, null, false);
    }

    private static SequentialTransition buildPhaseSequence(
            List<Message> messages, VBox labelsBox, StackPane overlay,
            Map<Integer, Runnable> onDisplayHooks) {
        return buildPhaseSequence(messages, labelsBox, overlay, onDisplayHooks, false);
    }

    private static SequentialTransition buildPhaseSequence(
            List<Message> messages, VBox labelsBox, StackPane overlay,
            Map<Integer, Runnable> onDisplayHooks, boolean initialOverlayVisible) {
        SequentialTransition seq = new SequentialTransition();
        boolean overlayVisible = initialOverlayVisible;
        // Build-time tracking of labels currently in labelsBox, used for FIFO fade-out
        java.util.List<Label> currentLabels = new java.util.ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            Message entry = messages.get(i);
            final Runnable displayHook = onDisplayHooks != null ? onDisplayHooks.get(i) : null;
            if (entry.appendBelow() && overlayVisible) {
                // ─ APPEND: fade a new label in underneath existing ones ───────────────
                Label newLabel = makeLabel(entry.text());
                if (entry.fadeIn()) newLabel.setOpacity(0);
                PauseTransition addNode = new PauseTransition(Duration.millis(1));
                addNode.setOnFinished(e -> {
                    labelsBox.getChildren().add(newLabel);
                    if (displayHook != null) displayHook.run();
                });
                PauseTransition hold = new PauseTransition(Duration.seconds(entry.displaySecs()));
                if (entry.fadeIn()) {
                    FadeTransition fadeLabel = new FadeTransition(Duration.millis(800), newLabel);
                    fadeLabel.setFromValue(0.0);
                    fadeLabel.setToValue(1.0);
                    fadeLabel.setInterpolator(Interpolator.EASE_OUT);
                    seq.getChildren().addAll(addNode, fadeLabel, hold);
                } else {
                    seq.getChildren().addAll(addNode, hold);
                }
                currentLabels.add(newLabel);
                if (entry.gapSecs() > 0) {
                    if (entry.holdOverlay()) {
                        // Fade all tracked labels simultaneously; overlay stays up
                        ParallelTransition allFade = new ParallelTransition();
                        for (Label labelToFade : new java.util.ArrayList<>(currentLabels)) {
                            FadeTransition fo = new FadeTransition(Duration.millis(1200), labelToFade);
                            fo.setFromValue(1.0);
                            fo.setToValue(0.0);
                            fo.setInterpolator(Interpolator.EASE_IN);
                            allFade.getChildren().add(fo);
                        }
                        PauseTransition gap = new PauseTransition(Duration.seconds(entry.gapSecs()));
                        seq.getChildren().addAll(allFade, gap);
                        currentLabels.clear();
                        // overlayVisible stays true
                    } else {
                        // Fade the whole overlay
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(1200), overlay);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setInterpolator(Interpolator.EASE_IN);
                        PauseTransition gap = new PauseTransition(Duration.seconds(entry.gapSecs()));
                        seq.getChildren().addAll(fadeOut, gap);
                        overlayVisible = false;
                        currentLabels.clear();
                    }
                }
                // skip the shared gapSecs block below
                continue;
            } else if (!overlayVisible) {
                // ─ NORMAL start: overlay hidden — clear, add label, fade overlay in ───
                Label newLabel = makeLabel(entry.text());
                currentLabels.clear();
                currentLabels.add(newLabel);
                PauseTransition setup = new PauseTransition(Duration.millis(1));
                setup.setOnFinished(e -> {
                    labelsBox.getChildren().clear();
                    labelsBox.getChildren().add(newLabel);
                    if (displayHook != null) displayHook.run();
                });
                PauseTransition hold = new PauseTransition(Duration.seconds(entry.displaySecs()));
                if (entry.fadeIn()) {
                    FadeTransition fadeInAnim = new FadeTransition(Duration.millis(800), overlay);
                    fadeInAnim.setFromValue(0.0);
                    fadeInAnim.setToValue(1.0);
                    fadeInAnim.setInterpolator(Interpolator.EASE_OUT);
                    seq.getChildren().addAll(setup, fadeInAnim, hold);
                } else {
                    PauseTransition appear = new PauseTransition(Duration.millis(1));
                    appear.setOnFinished(ev -> overlay.setOpacity(1.0));
                    seq.getChildren().addAll(setup, appear, hold);
                }
                overlayVisible = true;
            } else {
                // ─ CUT: overlay visible — swap label, optionally fade in ────────────────────
                Label newLabel = makeLabel(entry.text());
                if (entry.fadeIn()) newLabel.setOpacity(0);
                currentLabels.clear();
                currentLabels.add(newLabel);
                PauseTransition swap = new PauseTransition(Duration.millis(1));
                swap.setOnFinished(e -> {
                    labelsBox.getChildren().clear();
                    labelsBox.getChildren().add(newLabel);
                    if (displayHook != null) displayHook.run();
                });
                PauseTransition hold = new PauseTransition(Duration.seconds(entry.displaySecs()));
                if (entry.fadeIn()) {
                    FadeTransition fadeInLabel = new FadeTransition(Duration.millis(800), newLabel);
                    fadeInLabel.setFromValue(0.0);
                    fadeInLabel.setToValue(1.0);
                    fadeInLabel.setInterpolator(Interpolator.EASE_OUT);
                    seq.getChildren().addAll(swap, fadeInLabel, hold);
                } else {
                    seq.getChildren().addAll(swap, hold);
                }
                // overlayVisible stays true
            }

            // Non-append: gapSecs either fades just the label (holdOverlay) or the whole overlay
            if (entry.gapSecs() > 0) {
                boolean isLast = (i == messages.size() - 1);
                if (entry.holdOverlay() && !currentLabels.isEmpty()) {
                    // Fade only the current label; overlay stays up for the next message
                    Label labelRef = currentLabels.get(currentLabels.size() - 1);
                    FadeTransition fadeOutLabel = new FadeTransition(Duration.millis(isLast ? 3600 : 1200), labelRef);
                    fadeOutLabel.setFromValue(1.0);
                    fadeOutLabel.setToValue(0.0);
                    fadeOutLabel.setInterpolator(Interpolator.EASE_IN);
                    PauseTransition gap = new PauseTransition(Duration.seconds(entry.gapSecs()));
                    seq.getChildren().addAll(fadeOutLabel, gap);
                    currentLabels.clear();
                    // overlayVisible stays true
                } else {
                    // Fade the whole overlay; if last message also fade label slowly in parallel
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(1200), overlay);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setInterpolator(Interpolator.EASE_IN);
                    PauseTransition gap = new PauseTransition(Duration.seconds(entry.gapSecs()));
                    if (isLast && !currentLabels.isEmpty()) {
                        Label labelRef = currentLabels.get(currentLabels.size() - 1);
                        FadeTransition fadeOutLabel = new FadeTransition(Duration.millis(3600), labelRef);
                        fadeOutLabel.setFromValue(1.0);
                        fadeOutLabel.setToValue(0.0);
                        fadeOutLabel.setInterpolator(Interpolator.EASE_IN);
                        seq.getChildren().addAll(new ParallelTransition(fadeOut, fadeOutLabel), gap);
                    } else {
                        seq.getChildren().addAll(fadeOut, gap);
                    }
                    overlayVisible = false;
                    currentLabels.clear();
                }
            }
            // gapSecs == 0: no fade-out, overlayVisible unchanged
        }
        return seq;
    }

    /**
     * Plays {@code msgs} with button-repositioning hooks and an applause break before
     * "Thank you, thank you.". Falls back to a plain sequence if the marker messages
     * are absent.
     */
    private static void playSequenceWithButtonHooks(
            List<Message> msgs, VBox labelsBox, StackPane overlay,
            MediaPlayer applausePlayer,
            Runnable moveButtonToCenter, Runnable moveButtonBack,
            Runnable onComplete) {

        int closerIdx = -1, bringBackIdx = -1, thanksIdx = -1;
        for (int i = 0; i < msgs.size(); i++) {
            String t = msgs.get(i).text();
            if (t.equals("Here let me bring it closer"))  closerIdx   = i;
            if (t.equals("Let me bring the button back")) bringBackIdx = i;
            if (t.equals("Thank you, thank you."))         thanksIdx    = i;
        }

        Map<Integer, Runnable> hooks = new HashMap<>();
        if (closerIdx   >= 0) hooks.put(closerIdx,   moveButtonToCenter);
        if (bringBackIdx >= 0) hooks.put(bringBackIdx, moveButtonBack);

        if (bringBackIdx < 0) {
            // No button hooks needed — play the whole list normally
            SequentialTransition seq = buildPhaseSequence(msgs, labelsBox, overlay, null);
            if (onComplete != null) seq.setOnFinished(e -> onComplete.run());
            seq.play();
            return;
        }

        if (thanksIdx < 0 || thanksIdx <= bringBackIdx) {
            // No applause split — play with hooks but without splitting
            SequentialTransition seq = buildPhaseSequence(msgs, labelsBox, overlay, hooks);
            if (onComplete != null) seq.setOnFinished(e -> onComplete.run());
            seq.play();
            return;
        }

        List<Message> partA = msgs.subList(0, thanksIdx);
        List<Message> partB = msgs.subList(thanksIdx, msgs.size());

        SequentialTransition seqA = buildPhaseSequence(partA, labelsBox, overlay, hooks);
        SequentialTransition seqB = buildPhaseSequence(partB, labelsBox, overlay, null);
        if (onComplete != null) seqB.setOnFinished(e -> onComplete.run());

        seqA.setOnFinished(e -> {
            if (applausePlayer != null &&
                    applausePlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                applausePlayer.setOnEndOfMedia(() -> Platform.runLater(seqB::play));
            } else {
                seqB.play();
            }
        });
        seqA.play();
    }

    private static MediaPlayer loadMediaPlayer(String resourcePath) {
        try {
            java.net.URL url = NoStocksView.class.getResource(resourcePath);
            if (url == null) return null;
            return new MediaPlayer(new Media(url.toExternalForm()));
        } catch (Exception e) {
            return null;
        }
    }
}
