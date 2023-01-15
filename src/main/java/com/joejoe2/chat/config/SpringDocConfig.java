package com.joejoe2.chat.config;

import com.joejoe2.chat.data.UserPublicProfile;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "Spring Chat API", version = "v0.0.1"))
@SecuritySchemes({
        @SecurityScheme(
                name = "jwt",
                scheme = "bearer",
                bearerFormat = "jwt",
                type = SecuritySchemeType.HTTP,
                in = SecuritySchemeIn.HEADER
        ),
        @SecurityScheme(
                name = "jwt-in-query",
                paramName = "access_token",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.QUERY
        )
})
@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().components(new Components()
                .addSchemas("Sender", getSchemaWithDifferentDescription(UserPublicProfile.class,
                        "profile of the sender"))
                .addSchemas("Receiver", getSchemaWithDifferentDescription(UserPublicProfile.class,
                        "profile of the receiver, null if the message is in public channel", true)));
    }

    private Schema getSchemaWithDifferentDescription(Class className, String description) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(
                        new AnnotatedType(className).resolveAsRef(false));
        return resolvedSchema.schema.description(description);
    }

    private Schema getSchemaWithDifferentDescription(Class className, String description, Boolean nullable) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(
                        new AnnotatedType(className).resolveAsRef(false));
        return resolvedSchema.schema.description(description).nullable(nullable);
    }
}
