package com.Group13_65CNTT.AutoGarage.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Customer search filtered by whether the customer has a sign-in account, against MySQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class CustomerAccountFilterIntegrationTest extends IntegrationTestSupport {
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;

    @BeforeEach
    void fixtures() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930401,'acct_filter','x','Khach','CUSTOMER')");
        jdbc.update("INSERT INTO customers(id,user_id,customer_code,full_name,phone,credit_limit,is_active)"
                + " VALUES(930401,930401,'KH-ACCT-1','AcctFilter Co Tai Khoan','0930401001',0,true)");
        jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,credit_limit,is_active)"
                + " VALUES(930402,'KH-ACCT-2','AcctFilter Vang Lai','0930401002',0,true)");
    }

    @Test
    @DisplayName("hasAccount=true chỉ trả khách có user_id, false chỉ trả khách chưa có, bỏ trống trả cả hai")
    void filtersByAccount() {
        assertThat(customers.searchCustomers("AcctFilter", null, true))
                .extracting(Customer::getId).containsExactly(930401L);
        assertThat(customers.searchCustomers("AcctFilter", null, false))
                .extracting(Customer::getId).containsExactly(930402L);
        assertThat(customers.searchCustomers("AcctFilter", null, null))
                .extracting(Customer::getId).containsExactlyInAnyOrder(930401L, 930402L);
    }
}
