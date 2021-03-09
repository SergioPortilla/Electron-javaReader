package com.colpatria.onebank.approval.data.constant;

import com.colpatria.onebank.approval.data.exception.ProductCodeEnumException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ProductCodesEnum {
	VISA("Visa", "0028", "Visa", "VISA CLÁSICA", 1),
	MASTER("Master", "0020", "Mastercard", "MASTER CLÁSICA", 2),
	AMEX("American Express", "0386", "American Express", "ONE REWARDS", 3),
	INSTALLMENT("", "0018", "Crédito de Libre Inversión", "Instalamento", 4),
	REVOLVING("", "0016", "Crédito Rotativo", "Rotativo", 5),
	ZERO_ACCOUNT("Maestro", "0315", "Cuenta Cero", "Cuenta AH Digital", 0),
	ROSTER_ACCOUNT("Maestro", "0007", "Cuenta Nómina", "Nomina Ahorro", 0),
	RENT_ACCOUNT("Visa", "0039", "Cuenta Renta Premium", "Rentapremium", 0);

	private String franchise;
	private String productCode;
	private String productName;
	private String productType;
	private int priority;

	ProductCodesEnum(String franchise, String productCode, String productName, String productType,
			int priority) {
		this.franchise = franchise;
		this.productCode = productCode;
		this.productName = productName;
		this.productType = productType;
		this.priority = priority;
	}

	public String getProductCode() {
		return productCode;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductType() {
		return productType;
	}

	public int getPriority() {
		return priority;
	}

	public String getFranchise() {
		return franchise;
	}

	public static ProductCodesEnum fromType(String key) throws ProductCodeEnumException {
		for (ProductCodesEnum anEnum : ProductCodesEnum.values()) {
			if (String.valueOf(anEnum.getProductType()).equals(key)) {
				return anEnum;
			}
		}
		throw new ProductCodeEnumException("Enum not valid for: " + key, key);
	}

	public static ProductCodesEnum byFranchise(String key) throws ProductCodeEnumException {
		for (ProductCodesEnum anEnum : ProductCodesEnum.values()) {
			if (String.valueOf(anEnum.getFranchise()).equals(key)) {
				return anEnum;
			}
		}
		throw new ProductCodeEnumException("FRANCHISE NAME Enum not valid for: " + key, key);
	}

	public static ProductCodesEnum fromRiskFranchise(String key) {
		for (ProductCodesEnum anEnum : ProductCodesEnum.values()) {
			if (anEnum.getFranchise().toUpperCase(Locale.ENGLISH).equals(key)) {
				return anEnum;
			}
		}
		return null;
	}
}
