package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.dto.LoanRangeDTO;
import com.manu.springboot_backend.model.LoanRange;
import com.manu.springboot_backend.repository.LoanRangeRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/loan-package")
public class LoanPackageController {

    private final LoanRangeRepository loanRangeRepository;

    public LoanPackageController(LoanRangeRepository loanRangeRepository) {
        this.loanRangeRepository = loanRangeRepository;
    }

    // ✅ Create a new loan range
    @PostMapping
    public ResponseEntity<LoanRangeDTO> createLoanRange(@RequestBody LoanRangeDTO loanRangeDTO) {
        LoanRange loanRange = convertToEntity(loanRangeDTO);
        LoanRange savedLoanRange = loanRangeRepository.save(loanRange);
        return ResponseEntity.ok(convertToDTO(savedLoanRange));
    }

    // ✅ Get all loan ranges
    @GetMapping
    public ResponseEntity<List<LoanRangeDTO>> getAllLoanRanges() {
        List<LoanRangeDTO> loanRanges = loanRangeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(loanRanges);
    }

    // ✅ Get a single loan range by ID
    @GetMapping("/{id}")
    public ResponseEntity<LoanRangeDTO> getLoanRangeById(@PathVariable Long id) {
        return loanRangeRepository.findById(id)
                .map(loanRange -> ResponseEntity.ok(convertToDTO(loanRange)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Update a loan range
    @PutMapping("/{id}")
    public ResponseEntity<LoanRangeDTO> updateLoanRange(@PathVariable Long id, @RequestBody LoanRangeDTO loanRangeDTO) {
        return loanRangeRepository.findById(id)
                .map(existingLoanRange -> {
                    existingLoanRange.setPackageName(loanRangeDTO.getPackageName());
                    existingLoanRange.setMinAmount(loanRangeDTO.getMinAmount());
                    existingLoanRange.setMaxAmount(loanRangeDTO.getMaxAmount());
                    existingLoanRange.setInterestRate(loanRangeDTO.getInterestRate());
                    existingLoanRange.setMinRepaymentPeriod(loanRangeDTO.getMinRepaymentPeriod());
                    existingLoanRange.setMaxRepaymentPeriod(loanRangeDTO.getMaxRepaymentPeriod());
                    existingLoanRange.setMinCreditScore(loanRangeDTO.getMinCreditScore());
                    existingLoanRange.setFrequency(loanRangeDTO.getFrequency());


                    LoanRange updatedLoanRange = loanRangeRepository.save(existingLoanRange);
                    return ResponseEntity.ok(convertToDTO(updatedLoanRange));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Delete a loan range
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoanRange(@PathVariable Long id) {
        if (!loanRangeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        loanRangeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ✅ Convert Entity to DTO
    private LoanRangeDTO convertToDTO(LoanRange loanRange) {
        return new LoanRangeDTO(
                loanRange.getId(),
                loanRange.getPackageName(),
                loanRange.getMinAmount(),
                loanRange.getMaxAmount(),
                loanRange.getInterestRate(),
                loanRange.getMinRepaymentPeriod(),
                loanRange.getMaxRepaymentPeriod(),
                loanRange.getMinCreditScore(),
                loanRange.getFrequency()
        );
    }

    // ✅ Convert DTO to Entity
    private LoanRange convertToEntity(LoanRangeDTO dto) {
        return new LoanRange(
                null, // Remove id since it's auto-generated
                dto.getPackageName(),
                dto.getMinAmount(),
                dto.getMaxAmount(),
                dto.getInterestRate(),
                dto.getMinRepaymentPeriod(),
                dto.getMaxRepaymentPeriod(),
                dto.getMinCreditScore(),
                dto.getFrequency()

        );
    }
}
