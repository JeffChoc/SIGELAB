package universidad.sigelab.controller;

import universidad.sigelab.model.DashboardResumen;
import universidad.sigelab.service.DashboardService;

public class DashboardController {

    private final DashboardService servicioDashboard = new DashboardService();

    public DashboardResumen obtener() {
        return servicioDashboard.obtener();
    }
}
