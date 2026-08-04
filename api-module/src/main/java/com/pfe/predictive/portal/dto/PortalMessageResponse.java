package com.pfe.predictive.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMessageResponse {
    private Long id;
    private Long ticketId;
    private String senderUsername;
    private boolean fromCustomer;
    private String body;
    private LocalDateTime createdAt;
}
