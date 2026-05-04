package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Strategy for filtering by course name keyword.
 */
public class CourseNameCriterion implements PaperSearchCriterion {
    private final String keyword;

    public CourseNameCriterion(String keyword) {
        this.keyword = keyword != null ? keyword.toLowerCase() : "";
    }

    @Override
    public boolean matches(PastPaper paper) {
        return keyword.isEmpty() || 
               (paper.getCourseName() != null && paper.getCourseName().toLowerCase().contains(keyword));
    }
}
