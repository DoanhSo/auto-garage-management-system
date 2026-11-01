ALTER TABLE employees
    ADD COLUMN salary_coefficient DECIMAL(4, 2) NOT NULL DEFAULT 1.00 AFTER base_salary;

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_salary_coefficient CHECK (salary_coefficient BETWEEN 0.50 AND 3.00);
