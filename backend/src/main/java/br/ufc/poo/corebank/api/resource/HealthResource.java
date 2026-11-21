package br.ufc.poo.corebank.api.resource;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Health", description = "Verificação de disponibilidade do Core Bank Service")
public class HealthResource {

    @GET
    @Operation(summary = "Status do serviço", description = "Retorna o status operacional do motor bancário de forma reativa com Mutiny")
    public Uni<Map<String, Object>> health() {
        return Uni.createFrom().item(() -> Map.of(
                "service", "core-bank-service",
                "status", "UP",
                "reactiveEngine", "SmallRye Mutiny",
                "timestamp", Instant.now().toString()));
    }
}
