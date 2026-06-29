package com.gstcompliance.integration;

import com.gstcompliance.dto.request.ReconcileRequest;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.service.GstrPortalService;
import com.gstcompliance.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReconciliationIntegrationTest {

    @Autowired
    private GstrPortalService gstrPortalService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void testReconciliation() {
        // Given
        String userId = "test@example.com";
        ReconcileRequest request = ReconcileRequest.builder()
                .month(1)
                .year(2026)
                .build();

        // When
        var invoices = invoiceService.getInvoicesForPeriod(userId, 1, 2026);
        var portalInvoices = gstrPortalService.fetchGstr2b(userId, 1, 2026);

        // Then
        assertThat(invoices).isNotNull();
        assertThat(portalInvoices).isNotNull();
    }
}