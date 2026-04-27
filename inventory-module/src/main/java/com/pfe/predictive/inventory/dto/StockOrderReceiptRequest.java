package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderReceiptRequest implements Serializable {
    private Integer quantityReceived;
    private String proofOfDelivery;
    private String notes;
}
