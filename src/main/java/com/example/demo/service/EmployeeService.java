package com.example.demo.service;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.dto.EmployeeTreeResponse;
import com.example.demo.exception.EmployeeNotFoundException;
import com.example.demo.exception.InvalidHierarchyException;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public EmployeeResponse create(EmployeeRequest req) {
        Employee emp = new Employee();
        emp.setName(req.name());
        emp.setDesignation(req.designation());
        emp.setSalary(req.salary());
        if (req.managerId() != null) {
            Employee manager = repository.findById(req.managerId())
                .orElseThrow(() -> new EmployeeNotFoundException(req.managerId()));
            emp.setManager(manager);
        }
        return toResponse(repository.save(emp));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee emp = findOrThrow(id);
        emp.setName(req.name());
        emp.setDesignation(req.designation());
        emp.setSalary(req.salary());
        if (req.managerId() != null) {
            if (req.managerId().equals(id)) {
                throw new InvalidHierarchyException("Employee cannot be their own manager");
            }
            if (wouldCreateCycle(id, req.managerId())) {
                throw new InvalidHierarchyException("Setting this manager would create a reporting cycle");
            }
            Employee manager = repository.findById(req.managerId())
                .orElseThrow(() -> new EmployeeNotFoundException(req.managerId()));
            emp.setManager(manager);
        } else {
            emp.setManager(null);
        }
        return toResponse(repository.save(emp));
    }

    public void delete(Long id) {
        Employee emp = findOrThrow(id);
        Employee parentManager = emp.getManager();
        // copy list before mutating to avoid ConcurrentModificationException
        List<Employee> reports = new ArrayList<>(emp.getReports());
        reports.forEach(report -> {
            report.setManager(parentManager);
            repository.save(report);
        });
        repository.delete(emp);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDirectReports(Long id) {
        return findOrThrow(id).getReports().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getReportingChain(Long id) {
        List<EmployeeResponse> chain = new ArrayList<>();
        Employee current = findOrThrow(id);
        while (current != null) {
            chain.add(toResponse(current));
            current = current.getManager();
        }
        return chain;
    }

    @Transactional(readOnly = true)
    public EmployeeTreeResponse getTree(Long id) {
        return toTree(findOrThrow(id));
    }

    private EmployeeTreeResponse toTree(Employee emp) {
        List<EmployeeTreeResponse> childTrees = emp.getReports().stream()
            .map(this::toTree)
            .toList();
        return new EmployeeTreeResponse(emp.getId(), emp.getName(), emp.getDesignation(), emp.getSalary(), childTrees);
    }

    private boolean wouldCreateCycle(Long employeeId, Long newManagerId) {
        Long current = newManagerId;
        while (current != null) {
            if (current.equals(employeeId)) return true;
            Employee mgr = repository.findById(current).orElse(null);
            current = (mgr != null && mgr.getManager() != null) ? mgr.getManager().getId() : null;
        }
        return false;
    }

    private Employee findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private EmployeeResponse toResponse(Employee emp) {
        Long managerId = emp.getManager() != null ? emp.getManager().getId() : null;
        String managerName = emp.getManager() != null ? emp.getManager().getName() : null;
        return new EmployeeResponse(emp.getId(), emp.getName(), emp.getDesignation(), emp.getSalary(), managerId, managerName);
    }
}
