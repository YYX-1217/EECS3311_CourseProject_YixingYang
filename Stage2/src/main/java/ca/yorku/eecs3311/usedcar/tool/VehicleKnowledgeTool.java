package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.result.VehicleKnowledge;
import java.util.Objects;

public final class VehicleKnowledgeTool implements Tool {
    private final VehicleKnowledgeRepository repository;
    public VehicleKnowledgeTool(VehicleKnowledgeRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }
    @Override public VehicleKnowledge execute(Object input) {
        if (!(input instanceof Vehicle vehicle)) throw new IllegalArgumentException("Knowledge tool requires a Vehicle");
        return repository.findByVehicle(vehicle.make(), vehicle.model(), vehicle.year());
    }
}
