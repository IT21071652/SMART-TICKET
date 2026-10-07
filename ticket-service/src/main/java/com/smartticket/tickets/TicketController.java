package com.smartticket.tickets;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${APP_ORIGIN:http://localhost:5173}")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping("/tickets")
    public Page<Ticket> tickets(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String priority,
                                @RequestParam(required = false, name = "q") String query,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        return service.search(status, category, priority, query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/stats")
    public TicketService.TicketStats stats() {
        return service.stats();
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket create(@Valid @RequestBody TicketService.CreateTicketRequest request) {
        return service.create(request);
    }
}
