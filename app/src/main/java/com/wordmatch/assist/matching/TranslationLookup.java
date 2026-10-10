package com.wordmatch.assist.matching;

/** Supplies a symmetric confidence score for two candidate translations. */
public interface TranslationLookup {
    double score(String first, String second);
}
