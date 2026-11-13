package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.dto.LoanRequestResponseDTO;
import com.manu.springboot_backend.service.LoanRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    @Autowired
    private LoanRequestService loanRequestService;

    @GetMapping("/paid-loans")
    public ResponseEntity<List<LoanRequestResponseDTO>> getPaidLoans() {
        List<LoanRequestResponseDTO> paidLoans = loanRequestService.getPaidLoans();
        return ResponseEntity.ok(paidLoans);
    }

    @GetMapping("/unpaid-loans")
    public ResponseEntity<List<LoanRequestResponseDTO>> getUnpaidLoans() {
        List<LoanRequestResponseDTO> unpaidLoans = loanRequestService.getUnpaidLoans();
        return ResponseEntity.ok(unpaidLoans);
    }

    @GetMapping("/loan-summary")
    public ResponseEntity<?> getLoanSummary() {
        long paidLoanCount = loanRequestService.getPaidLoanCount();
        long unpaidLoanCount = loanRequestService.getUnpaidLoanCount();

        Map<String, Long> summary = Map.of(
                "paidLoans", paidLoanCount,
                "unpaidLoans", unpaidLoanCount
        );

        return ResponseEntity.ok(summary);
    }




}
