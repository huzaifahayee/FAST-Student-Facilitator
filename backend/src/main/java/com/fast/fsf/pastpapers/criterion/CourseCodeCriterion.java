package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Strategy for filtering by course code keyword.
 */
public class CourseCodeCriterion implements PaperSearchCriterion {
    private final String keyword;

    public CourseCodeCriterion(String keyword) {
        this.keyword = keyword != null ? keyword.toLowerCase() : "";
    }

    @Override
    public boolean matches(PastPaper paper) {
        return keyword.isEmpty() || 
               (paper.getCourseCode() != null && paper.getCourseCode().toLowerCase().contains(keyword));
    }
}
