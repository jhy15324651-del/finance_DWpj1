package org.zerock.finance_dwpj1.controller.stock;



import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.finance_dwpj1.service.stock.KIS.KisApiTokenService;


@RestController
@RequiredArgsConstructor
public class KisTokenTestController {

    private final KisApiTokenService kisApiTokenService;

    @GetMapping("/kis/token")
    public String testToken() {
        return kisApiTokenService.getAccessToken();
    }
}
