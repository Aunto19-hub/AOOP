package com.seamline.repository;

import com.seamline.domain.ApprovalRequest;
import com.seamline.domain.Employee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();

    List<ApprovalRequest> findByProposerOrderByCreatedAtDesc(Employee proposer);
}
