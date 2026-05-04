package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Strategy for filtering by exam type.
 */
public class ExamTypeCriterion implements PaperSearchCriterion {
    private final String examType;

    public ExamTypeCriterion(String examType) {
        this.examType = examType;
    }

    @Override
    public boolean matches(PastPaper paper) {
        return examType == null || examType.isEmpty() || 
               (paper.getExamType() != null && paper.getExamType().equalsIgnoreCase(examType));
    }
}
