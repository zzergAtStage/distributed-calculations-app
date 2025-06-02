package com.zzergatstage.model;

/**
 * Represents a loan request message.
 *
 * @param loanId Unique identifier.
 * @param applicant Name of applicant.
 * @param amount Requested loan amount.
 */
public record LoanRequest (String loanId, String applicant, double amount) {}