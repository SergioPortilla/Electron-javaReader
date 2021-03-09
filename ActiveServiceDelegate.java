package com.colpatria.its.fenix.api.delegate.impl;

import com.colpatria.its.fenix.api.constant.ApplicationState;
import com.colpatria.its.fenix.api.constant.Documents;
import com.colpatria.its.fenix.api.constant.GeneralConstant;
import com.colpatria.its.fenix.api.delegate.IActivateServiceDelegate;
import com.colpatria.its.fenix.api.delegate.IDocumentsServiceDelegate;
import com.colpatria.its.fenix.api.dto.ApplicationDto;
import com.colpatria.its.fenix.api.dto.ItemDto;
import com.colpatria.its.fenix.api.dto.ResponseApiDto;
import com.colpatria.its.fenix.api.dto.UserDto;
import com.colpatria.its.fenix.api.enums.PortFolioProduct;
import com.colpatria.its.fenix.api.enums.ProcessStatus;
import com.colpatria.its.fenix.api.exception.ConflictServiceException;
import com.colpatria.its.fenix.api.service.*;
import com.colpatria.its.fenix.api.util.ContextJWTHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Service
@Slf4j
public class ActiveServiceDelegate implements IActivateServiceDelegate {

    @Autowired
    private IApplicationService applicationService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IEmisionService emisionService;

    @Autowired
    private ICRMService CRMService;
    
    @Autowired
    private IInsuranceService insuranceService;

    @Autowired
    private IDocumentsServiceDelegate documentsServiceDelegate;

    @Override
    public ResponseEntity activateCreditCard(Long idApplication) {
        return activateCreditCard(idApplication, ContextJWTHelper.getSubject());
    }

    /*
    * This method is just for job task for a normal flow must be use activateCreditCard(Long idApplication)
    * because this method take username from context and
    * activateCreditCard(Long idApplication, String username) take it from application
    */
    @Override
    public ResponseEntity activateCreditCard(Long idApplication, String username) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        ApplicationDto applicationDto = null;
        try {
            ResponseApiDto<ApplicationDto> app;
            applicationDto = applicationService.findApplication(idApplication);

            // Validacion de solicitud
            if (applicationDto == null) {
                log.error("No existe solicitud con ID: " + idApplication);
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }

            // Validacion de estados
            if (!applicationDto.getLastState().equals(ApplicationState.FULFILMENT_EXECUTED) && !applicationDto.getLastState().equals(ApplicationState.CC_ACTIVATED)
                    && !applicationDto.getLastState().equals(ApplicationState.INSURANCES_CREATED) && !applicationDto.getLastState().equals(ApplicationState.PRE_APPROVED_LEGALIZED)) {
                log.error("Estado no valido, solicitud numero: " + applicationDto.getApplicationBusinessId() + " - Estado: " + applicationDto.getLastState());
                headers.add("Process-Status-Code", ProcessStatus.INVALID_PROCESS_STATE.getValue().toString());
                return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
            }

            // Se activa la tarjeta en emision segun el flag
            if (!applicationDto.getFulfilment().isActiveComplete()) {
                log.info("Activando producto, solicitud numero: " + applicationDto.getApplicationBusinessId());
                ResponseEntity success = emisionService.activeCreditCardProduct(applicationDto);
                if (success.getStatusCode().equals(HttpStatus.OK)) {
                    applicationDto.getFulfilment().setActiveComplete(true);
                    applicationDto.setLastState(ApplicationState.CC_ACTIVATED);
                    applicationDto.setActivatedByUser(userService.getUser(username));
                    applicationDto.setActivationOfficeCode(applicationDto.getTakenByUser().getSalesAgent().getBranchOfficeCode() + " - " + applicationDto.getTakenByUser().getSalesAgent().getBranchOffice());
                    app = applicationService.createOrUpdateApplication(applicationDto);
                    if (app != null && app.getError() == false) applicationDto = app.getResult();
                } else {
                    headers.add(GeneralConstant.STATUS_CODE, ProcessStatus.ACTIVE_EXECUTION_FAILED.getValue().toString());
                    applicationService.updateSubState(applicationDto.getId(), applicationDto.getLastState(),applicationDto.getLastState(), ProcessStatus.ACTIVE_EXECUTION_FAILED_PRODUCT.getReasonPhrase());
                    return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
                }
            }

            // Se actualiza la oportunidad segun el flag y si tiene oportunidad
            if (!applicationDto.getFulfilment().isPreApprovedComplete()) {
                if (applicationDto.getPrincipalItem().getOpportunity() != null) {
                    log.info("Actualizando oportunidad, solicitud numero: " + applicationDto.getApplicationBusinessId());
                    ResponseEntity success = CRMService.updateOportunity(applicationDto);
                    if (success.getStatusCode().equals(HttpStatus.OK)) {
                        applicationDto.getFulfilment().setPreApprovedComplete(true);
                        applicationDto.setLastState(ApplicationState.PRE_APPROVED_LEGALIZED);
                        app = applicationService.createOrUpdateApplication(applicationDto);
                        if (app != null && app.getError() == false) applicationDto = app.getResult();
                    } else {
                        headers.add(GeneralConstant.STATUS_CODE, ProcessStatus.ACTIVE_EXECUTION_FAILED.getValue().toString());
                        applicationService.updateSubState(applicationDto.getId(), applicationDto.getLastState(), applicationDto.getLastState(), ProcessStatus.ACTIVE_EXECUTION_FAILED_PRE_APPROVED.getReasonPhrase());
                        return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
                    }
                } else {
                    applicationDto.getFulfilment().setPreApprovedComplete(true);
                    app = applicationService.createOrUpdateApplication(applicationDto);
                    if (app != null && app.getError() == false) applicationDto = app.getResult();
                }
            }

            // Se crean los seguros segun el flag y si la solicitud los tiene
            if (!applicationDto.getFulfilment().isInsurancesComplete()) {
                List<ItemDto> itemDtos = applicationDto.getProductsByPortfolioAndProduct(
                        PortFolioProduct.SG.getPortFolioCode(),
                        PortFolioProduct.SG.getProductCode()
                );
                if (itemDtos != null && !itemDtos.isEmpty()) {
                    log.info("Creando seguros, solicitud numero: " + applicationDto.getApplicationBusinessId());
                    boolean allInsurances = true;
                    for (ItemDto itemDto : itemDtos) {
                        documentsServiceDelegate.createDocumentCardif(applicationDto, itemDto.getSubProduct().getSubproductCode());
                        boolean success = insuranceService.legalizeInsurances(applicationDto, itemDto);
                        if (!success) {
                            allInsurances = false;
                            break;
                        }
                    }
                    if (allInsurances) {
                        applicationDto.getFulfilment().setInsurancesComplete(true);
                        applicationDto.setLastState(ApplicationState.INSURANCES_CREATED);
                        app = applicationService.createOrUpdateApplication(applicationDto);
                        if (app != null && app.getError() == false) applicationDto = app.getResult();
                    } else {
                        headers.add(GeneralConstant.STATUS_CODE, ProcessStatus.ACTIVE_EXECUTION_FAILED.getValue().toString());
                        applicationService.updateSubState(applicationDto.getId(), applicationDto.getLastState(), applicationDto.getLastState(), ProcessStatus.ACTIVE_EXECUTION_FAILED_INSURANCES.getReasonPhrase());
                        return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
                    }
                } else {
                    applicationDto.getFulfilment().setInsurancesComplete(true);
                    app = applicationService.createOrUpdateApplication(applicationDto);
                    if (app != null && app.getError() == false) applicationDto = app.getResult();
                }
            }

            if (applicationDto.getFulfilment().isCompleteAllActive()) {
                UserDto user = userService.getUser(username);
                applicationDto.setActivatedByUser(user);
                applicationDto.setActivationOfficeCode(applicationDto.getTakenByUser().getSalesAgent().getBranchOfficeCode() + " - " + applicationDto.getTakenByUser().getSalesAgent().getBranchOffice());
                applicationDto.setLastState(ApplicationState.COMPLETED);
                applicationService.createOrUpdateApplication(applicationDto);
                headers.add(GeneralConstant.STATUS_CODE, ProcessStatus.ACTIVE_EXECUTION_SUCCESS.getValue().toString());
            }
        } catch (Exception ex) {
            log.error("Error no controlado: ", ex);
            if (applicationDto != null)
                applicationService.updateSubState(applicationDto.getId(), applicationDto.getLastState(), applicationDto.getLastState(), ProcessStatus.ACTIVE_EXECUTION_FAILED.getReasonPhrase());
            throw new ConflictServiceException(ex.getMessage(), "Error activando la tarjeta, por favor vuelva a intentarlo");
        }
        return new ResponseEntity(headers, HttpStatus.OK);
    }

}
