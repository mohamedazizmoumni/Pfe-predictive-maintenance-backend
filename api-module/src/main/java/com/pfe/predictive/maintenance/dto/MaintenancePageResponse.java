package com.pfe.predictive.maintenance.dto;

import java.util.List;

public class MaintenancePageResponse {

    private List<MaintenanceDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    public MaintenancePageResponse() {
    }

    public MaintenancePageResponse(List<MaintenanceDto> content, long totalElements, int totalPages, int currentPage) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public List<MaintenanceDto> getContent() {
        return content;
    }

    public void setContent(List<MaintenanceDto> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
