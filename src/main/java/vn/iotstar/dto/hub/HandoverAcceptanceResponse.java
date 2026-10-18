package vn.iotstar.dto.hub;

public record HandoverAcceptanceResponse(Long handoverId, int expectedContainers, int acceptedContainers, boolean accepted) { }
