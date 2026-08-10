package com.linkfit.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/members")
    public String members() { return "members"; }

    @GetMapping("/staff")
    public String staff() { return "staff"; }

    @GetMapping("/classes")
    public String classes() { return "redirect:/staff"; }

    @GetMapping("/attendance")
    public String attendance() { return "attendance"; }

    @GetMapping("/consults")
    public String consults() { return "consults"; }

    @GetMapping("/feedback")
    public String feedback() { return "feedback"; }

    @GetMapping("/lockers")
    public String lockers() { return "lockers"; }

    @GetMapping("/reregistration")
    public String reregistration() { return "reregistration"; }

    @GetMapping("/gym-requests")
    public String gymRequests() { return "gym-requests"; }

    @GetMapping("/revenue")
    public String revenue() { return "revenue"; }

    @GetMapping("/products")
    public String products() { return "products"; }

    @GetMapping("/messages")
    public String messages() { return "messages"; }

    @GetMapping("/pt")
    public String pt() { return "pt"; }

    @GetMapping("/inbox")
    public String inbox() { return "inbox"; }

    @GetMapping("/cs")
    public String cs() { return "cs"; }

    @GetMapping("/crm-sales")
    public String crmSales() { return "crm-sales"; }

    @GetMapping("/announcements")
    public String announcements() { return "announcements"; }

    @GetMapping("/settings")
    public String settings() { return "settings"; }
}
