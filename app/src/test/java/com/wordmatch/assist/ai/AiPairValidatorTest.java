package com.wordmatch.assist.ai;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class AiPairValidatorTest {
    @Test public void rejectsNonFiniteAndOutOfRangeConfidence() {
        for (double confidence : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 1.01, -1}) {
            assertEquals(0, AiPairValidator.validate(Arrays.asList("苹果"), Arrays.asList("apple"),
                    Arrays.asList(new AiPairValidator.CandidatePair("苹果", "apple", confidence))).size());
        }
    }
    @Test
    public void acceptsOnlyExactVisibleHighConfidencePairs() {
        List<AiPairValidator.ValidatedPair> result = AiPairValidator.validate(
                Arrays.asList("类似的", "车费"),
                Arrays.asList("similar", "fare"),
                Arrays.asList(
                        new AiPairValidator.CandidatePair("类似的", "similar", 0.98),
                        new AiPairValidator.CandidatePair("不存在", "fare", 0.99),
                        new AiPairValidator.CandidatePair("车费", "fare", 0.70)
                )
        );

        assertEquals(1, result.size());
        assertEquals("类似的", result.get(0).getLeft());
        assertEquals("similar", result.get(0).getRight());
    }

    @Test
    public void rejectsConflictingOneToManyOutput() {
        List<AiPairValidator.ValidatedPair> result = AiPairValidator.validate(
                Arrays.asList("文章", "论文"),
                Arrays.asList("essay"),
                Arrays.asList(
                        new AiPairValidator.CandidatePair("文章", "essay", 0.99),
                        new AiPairValidator.CandidatePair("论文", "essay", 0.99)
                )
        );

        assertEquals(1, result.size());
    }

    @Test
    public void rejectsAmbiguousDuplicateVisibleWords() {
        List<AiPairValidator.ValidatedPair> result = AiPairValidator.validate(
                Arrays.asList("结果", "结果"),
                Arrays.asList("result"),
                Arrays.asList(new AiPairValidator.CandidatePair("结果", "result", 0.99))
        );

        assertEquals(0, result.size());
    }
}
