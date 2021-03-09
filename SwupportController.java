package com.bns.onebank.affiliatedcardbff.controller;

import com.bns.onebank.affiliatedcardbff.constants.ResourceMapping;
import com.bns.onebank.affiliatedcardbff.constants.TraceMessagesConstants;
import com.bns.onebank.affiliatedcardbff.delegate.ISupportDelegate;
import com.colpatria.digitalfactory.common.log.api.IOTrace;
import com.colpatria.digitalfactory.common.log.api.InboundTrace;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="salajorg@colpatria.com">Jorge Salazar</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping(ResourceMapping.SUPPORT)
@AllArgsConstructor
public class SupportController {

    private final ISupportDelegate supportDelegate;

    @InboundTrace(ioTrace = @IOTrace(
            startMsg = TraceMessagesConstants.SUPPORT_LOGS_START,
            endMsg = TraceMessagesConstants.SUPPORT_LOGS_END,
            errorMsg = TraceMessagesConstants.SUPPORT_LOGS_ERROR
    ))
    @GetMapping(value = ResourceMapping.LOGS)
    public ResponseEntity<List<String>> logs() throws IOException {
        return supportDelegate.logs();
    }

}
