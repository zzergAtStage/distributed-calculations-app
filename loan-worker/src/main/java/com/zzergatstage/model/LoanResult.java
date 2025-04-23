package com.zzergatstage.model;

/**
 * Represents the result of a loan processing operation.
 *
 * @param loanId Loan identifier.
 * @param status Processing status ("APPROVED" / "REJECTED").
 */
public record LoanResult(String loanId, String status) {}