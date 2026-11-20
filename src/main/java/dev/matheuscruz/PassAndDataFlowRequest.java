package dev.matheuscruz;

import java.util.List;

public record PassAndDataFlowRequest(Order order) {

    public record Order(String id, List<Line> lines) {
    }

    public record Line(String sku, int qty) {
    }
}
