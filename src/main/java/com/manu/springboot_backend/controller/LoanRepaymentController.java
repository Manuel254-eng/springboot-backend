package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.dto.LoanRepaymentDTO;
import com.manu.springboot_backend.model.LoanRepayment;
import com.manu.springboot_backend.model.LoanRequest;
import com.manu.springboot_backend.model.RepaymentStatus;
import com.manu.springboot_backend.model.User;
import com.manu.springboot_backend.repository.LoanRepaymentRepository;
import com.manu.springboot_backend.repository.LoanRequestRepository;
import com.manu.springboot_backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/loan-repayments")
public class LoanRepaymentController {

    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;

    public LoanRepaymentController(LoanRepaymentRepository loanRepaymentRepository,
                                   LoanRequestRepository loanRequestRepository,
                                   UserRepository userRepository) {
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.loanRequestRepository = loanRequestRepository;
        this.userRepository = userRepository;
    }

    // ✅ Make a Loan Repayment
    @PostMapping
    public ResponseEntity<Map<String, Object>> makeRepayment(@RequestBody LoanRepaymentDTO repaymentDTO) {
        // Find Loan Request
        Optional<LoanRequest> loanRequestOpt = loanRequestRepository.findById(repaymentDTO.getLoanRequestId());
        if (loanRequestOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Loan request not found."));
        }
        LoanRequest loanRequest = loanRequestOpt.get();
        User user = loanRequest.getUser();

        // Fetch previous repayments for this loan request
        List<LoanRepayment> previousRepayments = loanRepaymentRepository.findByLoanRequest(loanRequest);

        // Calculate total amount paid so far
        double totalPaidSoFar = previousRepayments.stream().mapToDouble(LoanRepayment::getAmountPaid).sum();

        // Calculate remaining balance
        double remainingBalance = loanRequest.getExpectedPaymentAmount() - totalPaidSoFar;
        remainingBalance = Math.max(remainingBalance, 0); // Ensure balance doesn't go negative

        // ❌ Reject if the loan is already settled
        if (remainingBalance == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Loan already settled."));
        }

        // ❌ Reject if payment is more than remaining balance
        if (repaymentDTO.getAmount() > remainingBalance) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment exceeds the remaining balance.", "remainingBalance", remainingBalance));
        }

        // ✅ Create new repayment record
        double newRemainingBalance = remainingBalance - repaymentDTO.getAmount();
        LoanRepayment newRepayment = new LoanRepayment();
        newRepayment.setLoanRequest(loanRequest);
        newRepayment.setUser(user);
        newRepayment.setInstallmentAmount(loanRequest.getExpectedPaymentAmount() / loanRequest.getRepaymentPeriod()); // Expected installment
        newRepayment.setAmountPaid(repaymentDTO.getAmount());
        newRepayment.setRemainingBalance(newRemainingBalance);
        newRepayment.setStatus(newRemainingBalance <= 0 ? RepaymentStatus.PAID : RepaymentStatus.PENDING);
        newRepayment.setRepaymentMadeOn(LocalDate.now());

        // Save new repayment record
        loanRepaymentRepository.save(newRepayment);

        // ✅ Response structure
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Repayment recorded successfully.");
        response.put("amountPaid", repaymentDTO.getAmount());
        response.put("remainingBalance", newRemainingBalance);
        response.put("status", newRepayment.getStatus());

        return ResponseEntity.ok(response);
    }
}
