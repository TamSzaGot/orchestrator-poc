package com.example.adminbff.inbound;

import com.example.adminbff.core.TriggerRequest;
import com.example.adminbff.core.AdminBffUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminBffController {

    private final AdminBffUseCase useCase;

    public AdminBffController(AdminBffUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/jobs")
    public ResponseEntity<String> getJobs() {
        return ResponseEntity.status(200).body(useCase.getJobs());
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(@RequestBody TriggerRequest request) {
        return ResponseEntity.ok(useCase.trigger(request));
    }
}
