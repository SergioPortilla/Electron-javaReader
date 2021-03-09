package com.colpatria.its.fenix.api.delegate;

import org.springframework.http.ResponseEntity;

public interface IActivateServiceDelegate {


    /**
     * Active credit card.
     *
     * @param idApplication
     * @return Boolean
     */
    ResponseEntity activateCreditCard(Long idApplication);

    ResponseEntity activateCreditCard(Long idApplication, String username);

}
