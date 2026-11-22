package com.ecardfolio.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// [AI assisted chat-017] 同一網域部署時，讓公開名片深層連結回到 Vue SPA。
@Controller
public class FrontendPageController {
    @GetMapping({"/", "/card/{cardId}"})
    public String frontend() { return "forward:/index.html"; }
}
