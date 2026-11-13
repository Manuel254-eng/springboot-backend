package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.dto.LoanRequestDTO;
import com.manu.springboot_backend.dto.LoanRequestResponseDTO;
import com.manu.springboot_backend.model.*;
import com.manu.springboot_backend.repository.*;
import com.manu.springboot_backend.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/loan-requests")
public class LoanRequestController {

    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;
    private final LoanRangeRepository loanRangeRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanRequestService loanRequestService;

    public LoanRequestController(LoanRequestRepository loanRequestRepository, UserRepository userRepository, LoanRangeRepository loanRangeRepository, RepaymentScheduleRepository repaymentScheduleRepository, LoanRepaymentRepository loanRepaymentRepository, LoanRequestService loanRequestService) {
        this.loanRequestRepository = loanRequestRepository;
        this.userRepository = userRepository;
        this.loanRangeRepository = loanRangeRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.loanRequestService = loanRequestService;
    }



    // ✅ Create a new loan request (User must be authenticated)
    @Operation(summary = "Request a loan", description = "Allows users to request a loan. Only the amount is required.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> requestLoan(@RequestBody LoanRequestDTO loanRequestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        User user = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasPendingRequest = loanRequestRepository.existsByUserAndStatus(user, LoanStatus.PENDING);
        if (hasPendingRequest) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "You already have a pending loan request."));
        }

        double principal = loanRequestDTO.getAmount();

        LoanRange loanRange = loanRangeRepository.findByMinAmountLessThanEqualAndMaxAmountGreaterThanEqual(principal, principal)
                .orElseThrow(() -> new RuntimeException("No loan range found for this amount"));

        int minCreditScoreRequired = loanRange.getMinCreditScore();
        String frequency = String.valueOf(loanRange.getFrequency());
        double interestRate = loanRange.getInterestRate();
        int repaymentPeriod = loanRange.getMinRepaymentPeriod();

        // ✅ Calculate total interest
        double totalInterest = (principal * interestRate * repaymentPeriod) / 100;

        // ✅ Calculate total repayment amount
        double expectedPaymentAmount = principal + totalInterest;

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setUser(user);
        loanRequest.setAmount(principal);
        loanRequest.setUserCreditScore((int) Math.round(user.getCreditScore()));
        loanRequest.setMinCreditScoreRequired(minCreditScoreRequired);
        loanRequest.setFrequency(frequency);
        loanRequest.setInterestRate(interestRate);
        loanRequest.setStatus(LoanStatus.PENDING);
        loanRequest.setRepaymentPeriod(repaymentPeriod);
        loanRequest.setExpectedPaymentAmount(expectedPaymentAmount); // ✅ Save expected amount

        loanRequestRepository.save(loanRequest);

        return ResponseEntity.ok(Map.of("message", "Loan request submitted successfully"));
    }


    @Operation(summary = "Fetch all pending loan requests",
        description = "Retrieves all loan requests that are currently in PENDING status. Only accessible by an admin.")
@GetMapping("/pending")
public ResponseEntity<?> getPendingLoanRequests() {
    if (!isAdmin()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You are not authorized to view this data."));
    }

    List<LoanRequest> pendingLoanRequests = loanRequestRepository.findByStatus(LoanStatus.PENDING);

    return ResponseEntity.ok(pendingLoanRequests);
}






    @PutMapping("/{loanRequestId}/status")
    public ResponseEntity<Map<String, Object>> updateLoanStatus(
            @PathVariable Long loanRequestId,
            @RequestParam LoanStatus status) {

        if (status != LoanStatus.APPROVED && status != LoanStatus.REJECTED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid status. Use APPROVED or REJECTED."));
        }

        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to perform this action."));
        }

        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        if (loanRequest.getStatus() != LoanStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "This loan request has already been processed."));
        }

        loanRequest.setStatus(status);
        loanRequest.setApprover(getLoggedInUser());

        if (status == LoanStatus.APPROVED) {
            generateRepaymentSchedule(loanRequest);
        }

        loanRequestRepository.save(loanRequest);

        return ResponseEntity.ok(Map.of("message", "Loan request updated successfully", "status", status));
    }

    private void generateRepaymentSchedule(LoanRequest loanRequest) {
        int repaymentPeriod = loanRequest.getRepaymentPeriod();
        String frequency = loanRequest.getFrequency().toLowerCase();
        double principalAmount = loanRequest.getAmount();
        double interestRate = loanRequest.getInterestRate();

        // ✅ Calculate total interest
        double totalInterest = (principalAmount * interestRate * repaymentPeriod) / 100;

        // ✅ Calculate total amount to be repaid
        double totalRepaymentAmount = principalAmount + totalInterest;

        // ✅ Calculate installment amount
        double installmentAmount = totalRepaymentAmount / repaymentPeriod;

        LocalDate startDate = LocalDate.now();
        int interval = 1; // Default interval

        System.out.println("Frequency: " + frequency);

        switch (frequency) {
            case "weekly":
                interval = 7; // Every 7 days
                break;
            case "monthly":
                interval = 1; // Every month
                break;
            case "yearly":
                interval = 12; // Every year
                break;
            default:
                throw new IllegalArgumentException("Invalid repayment frequency");
        }

        System.out.println("Interval: " + interval);

        for (int i = 1; i <= repaymentPeriod; i++) {
            LocalDate dueDate;
            if (frequency.equals("weekly")) {
                dueDate = startDate.plusWeeks(i);
            } else if (frequency.equals("monthly")) {
                dueDate = startDate.plusMonths(i);
            } else { // Yearly
                dueDate = startDate.plusYears(i);
            }

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanRequest(loanRequest);
            schedule.setUser(loanRequest.getUser());
            schedule.setScheduledRepaymentDate(dueDate);
            schedule.setInstallmentAmount(installmentAmount);
            schedule.setStatus(RepaymentStatus.PENDING);

            repaymentScheduleRepository.save(schedule);
        }
    }
    @Operation(summary = "Fetch all loan requests for a user",
            description = "Retrieves all loan requests made by a specific user, including principal, total repayment, amount paid, and remaining balance. Only accessible by the user or an admin.")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserLoanRequests(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        // Get the logged-in user
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the logged-in user is an admin or the requested user
        if (!isAdmin() && !loggedInUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view this data."));
        }

        // Fetch loan requests by user ID
        List<LoanRequest> loanRequests = loanRequestRepository.findByUserId(userId);

        // Create a response list
        List<Map<String, Object>> response = new ArrayList<>();

        for (LoanRequest loan : loanRequests) {
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("principal", loan.getAmount()); // ✅ Borrowed amount
            loanData.put("totalRepayment", loan.getExpectedPaymentAmount()); // ✅ Total repayment amount

            // Get the latest repayment record for this loan request
            Optional<LoanRepayment> latestRepayment = loanRepaymentRepository
                    .findTopByLoanRequestIdOrderByCreatedAtDesc(loan.getId());

            double remainingBalance;

            if (latestRepayment.isPresent()) {
                remainingBalance = latestRepayment.get().getRemainingBalance();
            } else {
                remainingBalance = loan.getExpectedPaymentAmount(); // No payment made, full amount due
            }

            double amountPaid = loan.getExpectedPaymentAmount() - remainingBalance; // ✅ Correct calculation

            loanData.put("amountPaid", amountPaid); // ✅ Total amount already paid
            loanData.put("remainingBalance", remainingBalance); // ✅ Latest balance
            loanData.put("loanStatus", loan.getStatus()); // ✅ Loan status

            response.add(loanData);
        }

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Fetch repayment schedule for a loan", 
            description = "Retrieves the repayment schedule for a specific loan request. Only accessible by the loan owner or an admin.")
    @GetMapping("/{loanRequestId}/repayment-schedule")
    public ResponseEntity<?> getRepaymentSchedule(@PathVariable Long loanRequestId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        // Get the logged-in user
        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the loan request
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        // Check if the logged-in user is an admin or the owner of the loan request
        if (!isAdmin() && !loanRequest.getUser().getId().equals(loggedInUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view this data."));
        }

        // Fetch repayment schedule entries for the loan request
        List<RepaymentSchedule> repaymentSchedules = repaymentScheduleRepository.findByLoanRequest(loanRequest);

        // Map response
        List<Map<String, Object>> response = new ArrayList<>();
        for (RepaymentSchedule schedule : repaymentSchedules) {
            Map<String, Object> scheduleData = new HashMap<>();
            scheduleData.put("dueDate", schedule.getScheduledRepaymentDate());
            scheduleData.put("installmentAmount", schedule.getInstallmentAmount());
            scheduleData.put("status", schedule.getStatus());

            response.add(scheduleData);
        }

        return ResponseEntity.ok(response);
    }


    // 🔹 Helper method to check if logged-in user is ADMIN
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(loggedInEmail);
        return userOpt.isPresent() && userOpt.get().getRole() == Role.ADMIN;
    }

    // 🔹 Helper method to get the logged-in user
    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        return userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
