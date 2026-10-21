package universidad.sigelab.service;

import universidad.sigelab.model.DashboardResumen;
import universidad.sigelab.repository.DashboardRepository;
import universidad.sigelab.util.SesionUsuario;

public class DashboardService {

    private final DashboardRepository repositorioDashboard = new DashboardRepository();

    public DashboardResumen obtener() {
        SesionUsuario.exigirSesion();
        return repositorioDashboard.obtener();
    }
}
