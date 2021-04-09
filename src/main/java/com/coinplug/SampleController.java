package com.coinplug;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sample")
class SampleController {

    @PostMapping
    public String sample(@RequestBody String request) {

        return "Hello world";
    }
}
