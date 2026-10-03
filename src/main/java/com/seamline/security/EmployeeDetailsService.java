package com.seamline.security;

import com.seamline.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads an employee by whatever the sign-in ticket was given: email first, then mobile number. */
@Service
public class EmployeeDetailsService implements UserDetailsService {

    private final EmployeeRepository employees;

    public EmployeeDetailsService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return employees.findByEmailIgnoreCase(identifier)
                .or(() -> employees.findByPhone(identifier))
                .map(SeamlineUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No employee with email or mobile number " + identifier));
    }
}
