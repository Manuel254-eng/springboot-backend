package com.manu.springboot_backend.repository;

import com.manu.springboot_backend.model.LoanRequest;
import com.manu.springboot_backend.model.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanRequest(LoanRequest loanRequest);
    
}
