package com.seamline.repository;

import com.seamline.domain.Employee;
import com.seamline.domain.OperatorRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRequestRepository extends JpaRepository<OperatorRequest, Long> {

    List<OperatorRequest> findAllByOrderByCreatedAtDesc();

    List<OperatorRequest> findByRequesterOrderByCreatedAtDesc(Employee requester);
}
