// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun ledgerOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("FinCore Ledger API")
                .version("0.1.0")
                .license(
                    License()
                        .name("BUSL-1.1")
                        .url("https://github.com/tiana-code/fincore-engine/blob/main/LICENSE"),
                ),
        )
}
