package com.cozumtr.leave_management_system.dto.response;

import com.cozumtr.leave_management_system.entities.Employee;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String departmentName;
    private String roleName;

    // Entity -> DTO Çevirici Yapıcı Metot
    public UserResponse(Employee employee) {
        this.id = employee.getId();
        this.email = employee.getEmail();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.phoneNumber = employee.getPhoneNumber();
        this.address = employee.getAddress();

        // Null hatası almamak için kontroller
        if (employee.getDepartment() != null) {
            this.departmentName = employee.getDepartment().getName();
        }

        if (employee.getUser() != null && employee.getUser().getRoles() != null && !employee.getUser().getRoles().isEmpty()) {
            this.roleName = employee.getUser().getRoles().iterator().next().getRoleName();
        }
    }
}
