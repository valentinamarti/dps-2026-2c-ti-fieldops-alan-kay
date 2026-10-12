package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description of the API for springdoc. The document is served at {@code /v3/api-docs} and Swagger UI at
 * {@code /swagger-ui.html}; springdoc builds both from the controllers, so there is nothing to keep in sync.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI fieldOpsApi() {
        return new OpenAPI().info(new Info()
                .title("Field Ops API")
                .version("1.0")
                .description("REST adapter of the Field Ops hexagon: it exposes the use cases of the domain "
                        + "and translates its exceptions to HTTP with ProblemDetail (D49)."));
    }
}
