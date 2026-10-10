package com.wordmatch.assist.matching;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory O(1) vocabulary index backed by bundled, imported, manual and learned pairs. */
public final class VocabularyStore implements TranslationLookup {
    public static final String USER_FILE_NAME = "user_vocabulary.csv";
    private static final String LEARNED_FILE_NAME = "learned_vocabulary.tsv";
    private static final String MANUAL_FILE_NAME = "manual_vocabulary.tsv";
    private static final String AI_FILE_NAME = "ai_vocabulary.tsv";

    private final Context context;
    private final Map<String, Set<String>> translations = new HashMap<>();
    private final List<VocabularyPair> learnedPairs = new ArrayList<>();
    private final List<VocabularyPair> manualPairs = new ArrayList<>();
    private final List<VocabularyPair> aiPairs = new ArrayList<>();

    public VocabularyStore(Context context) {
        this.context = context.getApplicationContext();
        loadAll();
    }

    private void loadAll() {
        translations.clear();
        learnedPairs.clear();
        manualPairs.clear();
        aiPairs.clear();
        try (InputStream stream = context.getAssets().open("vocabulary.csv")) {
            loadStream(stream, null);
        } catch (IOException ignored) {
            // The app remains usable with an imported, manual or learned vocabulary.
        }

        File userFile = new File(context.getFilesDir(), USER_FILE_NAME);
        if (userFile.isFile()) {
            try (InputStream stream = new FileInputStream(userFile)) {
                loadStream(stream, null);
            } catch (IOException ignored) {
                // A malformed optional file must not stop screen recognition.
            }
        }

        loadTrackedFile(new File(context.getFilesDir(), MANUAL_FILE_NAME), manualPairs);
        loadTrackedFile(new File(context.getFilesDir(), LEARNED_FILE_NAME), learnedPairs);
        loadTrackedFile(new File(context.getFilesDir(), AI_FILE_NAME), aiPairs);
    }

    private void loadTrackedFile(File file, List<VocabularyPair> destination) {
        if (!file.isFile()) {
            return;
        }
        try (InputStream stream = new FileInputStream(file)) {
            loadStream(stream, destination);
            // Migrate old files by removing duplicate and malformed rows.
            rewritePairs(file, destination);
        } catch (IOException ignored) {
            // Continue with every pair loaded before the error.
        }
    }

    private void loadStream(InputStream stream, List<VocabularyPair> trackedPairs)
            throws IOException {
        Set<String> trackedKeys = new HashSet<>();
        if (trackedPairs != null) {
            for (VocabularyPair pair : trackedPairs) {
                trackedKeys.add(pairKey(pair.first, pair.second));
            }
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] columns = trimmed.split("[\\t,;]", 2);
                if (columns.length != 2) {
                    continue;
                }
                PairValues values = normalizePair(columns[0], columns[1]);
                if (values == null) {
                    continue;
                }
                addToIndex(values.normalizedFirst, values.normalizedSecond);
                if (trackedPairs != null && trackedKeys.add(values.key)) {
                    trackedPairs.add(new VocabularyPair(values.first, values.second));
                }
            }
        }
    }

    private void addToIndex(String first, String second) {
        addDirection(first, second);
        addDirection(second, first);
    }

    private void addDirection(String source, String target) {
        Set<String> targets = translations.get(source);
        if (targets == null) {
            targets = new HashSet<>();
            translations.put(source, targets);
        }
        targets.add(target);
    }

    public boolean addLearnedPair(String first, String second) {
        PairValues values = normalizePair(first, second);
        if (values == null || exactMatch(values.normalizedFirst, values.normalizedSecond)) {
            return false;
        }
        File learnedFile = new File(context.getFilesDir(), LEARNED_FILE_NAME);
        if (!appendPair(learnedFile, values)) {
            return false;
        }
        addToIndex(values.normalizedFirst, values.normalizedSecond);
        learnedPairs.add(new VocabularyPair(values.first, values.second));
        return true;
    }

    /**
     * Persists a user-confirmed pair and removes contradictory learned/model rows that were
     * simultaneously visible when the user clicked. Limiting correction to the current board
     * preserves legitimate translations that did not participate in this exercise.
     */
    public LearnResult confirmLearnedPair(
            String first,
            String second,
            List<String> visibleAtClick
    ) {
        PairValues values = normalizePair(first, second);
        if (values == null) {
            return LearnResult.failure();
        }
        Set<String> visible = new HashSet<>();
        if (visibleAtClick != null) {
            for (String word : visibleAtClick) {
                String normalized = WordNormalizer.normalize(word);
                if (!normalized.isEmpty()) {
                    visible.add(normalized);
                }
            }
        }
        List<VocabularyPair> correctedLearned = withoutVisibleConflicts(
                learnedPairs,
                values,
                visible
        );
        List<VocabularyPair> correctedAi = withoutVisibleConflicts(aiPairs, values, visible);
        int conflictsRemoved = learnedPairs.size() - correctedLearned.size()
                + aiPairs.size() - correctedAi.size();
        boolean alreadyLearned = containsPair(correctedLearned, values.key);
        if (!alreadyLearned) {
            correctedLearned.add(new VocabularyPair(values.first, values.second));
        }
        try {
            rewritePairs(
                    new File(context.getFilesDir(), LEARNED_FILE_NAME),
                    correctedLearned
            );
            rewritePairs(new File(context.getFilesDir(), AI_FILE_NAME), correctedAi);
            loadAll();
            return LearnResult.success(!alreadyLearned, conflictsRemoved);
        } catch (IOException ignored) {
            return LearnResult.failure();
        }
    }

    public AddResult addManualPair(String first, String second) {
        PairValues values = normalizePair(first, second);
        if (values == null) {
            return AddResult.INVALID;
        }
        if (exactMatch(values.normalizedFirst, values.normalizedSecond)) {
            return AddResult.ALREADY_EXISTS;
        }
        File manualFile = new File(context.getFilesDir(), MANUAL_FILE_NAME);
        if (!appendPair(manualFile, values)) {
            return AddResult.IO_ERROR;
        }
        addToIndex(values.normalizedFirst, values.normalizedSecond);
        manualPairs.add(new VocabularyPair(values.first, values.second));
        return AddResult.ADDED;
    }

    public AddResult addAiPair(String first, String second) {
        PairValues values = normalizePair(first, second);
        if (values == null) {
            return AddResult.INVALID;
        }
        if (exactMatch(values.normalizedFirst, values.normalizedSecond)) {
            return AddResult.ALREADY_EXISTS;
        }
        File aiFile = new File(context.getFilesDir(), AI_FILE_NAME);
        if (!appendPair(aiFile, values)) {
            return AddResult.IO_ERROR;
        }
        addToIndex(values.normalizedFirst, values.normalizedSecond);
        aiPairs.add(new VocabularyPair(values.first, values.second));
        return AddResult.ADDED;
    }

    public boolean removeLearnedPair(VocabularyPair pair) {
        return removePair(
                new File(context.getFilesDir(), LEARNED_FILE_NAME),
                learnedPairs,
                pair
        );
    }

    public boolean removeManualPair(VocabularyPair pair) {
        return removePair(
                new File(context.getFilesDir(), MANUAL_FILE_NAME),
                manualPairs,
                pair
        );
    }

    public boolean removeAiPair(VocabularyPair pair) {
        return removePair(
                new File(context.getFilesDir(), AI_FILE_NAME),
                aiPairs,
                pair
        );
    }

    private boolean removePair(
            File file,
            List<VocabularyPair> source,
            VocabularyPair pairToRemove
    ) {
        if (pairToRemove == null) {
            return false;
        }
        String removeKey = pairKey(pairToRemove.first, pairToRemove.second);
        List<VocabularyPair> updated = new ArrayList<>();
        for (VocabularyPair pair : source) {
            if (!removeKey.equals(pairKey(pair.first, pair.second))) {
                updated.add(pair);
            }
        }
        if (updated.size() == source.size()) {
            return false;
        }
        try {
            rewritePairs(file, updated);
            // Rebuild the shared index too; otherwise a deleted row remains executable until
            // the accessibility service is restarted.
            loadAll();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private List<VocabularyPair> withoutVisibleConflicts(
            List<VocabularyPair> source,
            PairValues confirmed,
            Set<String> visible
    ) {
        List<VocabularyPair> output = new ArrayList<>();
        for (VocabularyPair pair : source) {
            if (VisiblePairConflictPolicy.shouldRemove(
                    pair.first,
                    pair.second,
                    confirmed.first,
                    confirmed.second,
                    visible
            )) {
                continue;
            }
            output.add(pair);
        }
        return output;
    }

    private boolean containsPair(List<VocabularyPair> pairs, String expectedKey) {
        for (VocabularyPair pair : pairs) {
            if (expectedKey.equals(pairKey(pair.first, pair.second))) {
                return true;
            }
        }
        return false;
    }

    private boolean appendPair(File file, PairValues values) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true),
                StandardCharsets.UTF_8
        ))) {
            writePair(writer, new VocabularyPair(values.first, values.second));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void rewritePairs(File file, List<VocabularyPair> pairs) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, false),
                StandardCharsets.UTF_8
        ))) {
            for (VocabularyPair pair : pairs) {
                writePair(writer, pair);
            }
        }
    }

    private void writePair(BufferedWriter writer, VocabularyPair pair) throws IOException {
        writer.write(sanitizeForFile(pair.first));
        writer.write('\t');
        writer.write(sanitizeForFile(pair.second));
        writer.newLine();
    }

    private String sanitizeForFile(String value) {
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private PairValues normalizePair(String first, String second) {
        String displayFirst = sanitizeForFile(first == null ? "" : first);
        String displaySecond = sanitizeForFile(second == null ? "" : second);
        String normalizedFirst = WordNormalizer.normalize(displayFirst);
        String normalizedSecond = WordNormalizer.normalize(displaySecond);
        if (normalizedFirst.isEmpty()
                || normalizedSecond.isEmpty()
                || normalizedFirst.equals(normalizedSecond)) {
            return null;
        }
        return new PairValues(
                displayFirst,
                displaySecond,
                normalizedFirst,
                normalizedSecond
        );
    }

    private static String pairKey(String first, String second) {
        String a = WordNormalizer.normalize(first);
        String b = WordNormalizer.normalize(second);
        return a.compareTo(b) <= 0 ? a + '\u0000' + b : b + '\u0000' + a;
    }

    @Override
    public double score(String first, String second) {
        String a = WordNormalizer.normalize(first);
        String b = WordNormalizer.normalize(second);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return 0.0;
        }
        if (exactMatch(a, b)) {
            return 1.0;
        }

        double best = fuzzyScore(a, b);
        best = Math.max(best, fuzzyScore(b, a));
        return best;
    }

    private boolean exactMatch(String first, String second) {
        Set<String> targets = translations.get(first);
        return targets != null && targets.contains(second);
    }

    private double fuzzyScore(String source, String observedTarget) {
        Set<String> targets = translations.get(source);
        if (targets == null) {
            targets = Collections.emptySet();
        }
        if (targets.isEmpty() || observedTarget.length() < 4) {
            return 0.0;
        }
        double best = 0.0;
        for (String target : targets) {
            if (Math.max(target.length(), observedTarget.length()) < 4) {
                continue;
            }
            double similarity = WordNormalizer.similarity(target, observedTarget);
            if (similarity >= 0.88) {
                best = Math.max(best, similarity * 0.95);
            }
        }
        return best;
    }

    public int size() {
        int directedCount = 0;
        for (Set<String> targets : translations.values()) {
            directedCount += targets.size();
        }
        return directedCount / 2;
    }

    public List<VocabularyPair> getLearnedPairsNewestFirst() {
        return newestFirst(learnedPairs);
    }

    public List<VocabularyPair> getManualPairsNewestFirst() {
        return newestFirst(manualPairs);
    }

    public List<VocabularyPair> getAiPairsNewestFirst() {
        return newestFirst(aiPairs);
    }

    private List<VocabularyPair> newestFirst(List<VocabularyPair> source) {
        List<VocabularyPair> result = new ArrayList<>(source);
        Collections.reverse(result);
        return result;
    }

    public static void replaceUserVocabulary(Context context, InputStream source) throws IOException {
        File destination = new File(context.getFilesDir(), USER_FILE_NAME);
        try (InputStream input = source; FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
        }
    }

    public enum AddResult {
        ADDED,
        ALREADY_EXISTS,
        INVALID,
        IO_ERROR
    }

    public static final class LearnResult {
        private final boolean success;
        private final boolean added;
        private final int conflictsRemoved;

        private LearnResult(boolean success, boolean added, int conflictsRemoved) {
            this.success = success;
            this.added = added;
            this.conflictsRemoved = conflictsRemoved;
        }

        private static LearnResult success(boolean added, int conflictsRemoved) {
            return new LearnResult(true, added, Math.max(0, conflictsRemoved));
        }

        private static LearnResult failure() {
            return new LearnResult(false, false, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean wasAdded() {
            return added;
        }

        public int getConflictsRemoved() {
            return conflictsRemoved;
        }
    }

    public static final class VocabularyPair {
        private final String first;
        private final String second;

        private VocabularyPair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public String getSecond() {
            return second;
        }

        public String getDisplayText() {
            return first + " ↔ " + second;
        }
    }

    private static final class PairValues {
        private final String first;
        private final String second;
        private final String normalizedFirst;
        private final String normalizedSecond;
        private final String key;

        private PairValues(
                String first,
                String second,
                String normalizedFirst,
                String normalizedSecond
        ) {
            this.first = first;
            this.second = second;
            this.normalizedFirst = normalizedFirst;
            this.normalizedSecond = normalizedSecond;
            this.key = pairKey(normalizedFirst, normalizedSecond);
        }
    }
}
