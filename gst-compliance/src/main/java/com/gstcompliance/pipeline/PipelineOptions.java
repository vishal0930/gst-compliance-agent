package com.gstcompliance.pipeline;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "pipeline")
public class PipelineOptions {
    private boolean enableHsnClassification = true;
    private boolean enableReconciliation = true;
    private boolean enableDeadlineTracking = true;
    private boolean enableReturnDrafting = true;
}
