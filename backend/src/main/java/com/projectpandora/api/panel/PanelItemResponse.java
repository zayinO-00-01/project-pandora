package com.projectpandora.api.panel;

public record PanelItemResponse(Long id, String title, Integer sortOrder, String myRemark, String detail) {
    public PanelItemResponse(Long id, String title, Integer sortOrder, String myRemark) {
        this(id, title, sortOrder, myRemark, null);
    }
}
