package ge.comcom.anubis.controller.dashboard;

import ge.comcom.anubis.dto.dashboard.*;
import ge.comcom.anubis.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/distribution")
    public List<DashboardDistributionDto> getDistribution() {
        return dashboardService.getDistribution();
    }

    @GetMapping("/activity")
    public DashboardActivityDto getActivity(@RequestParam(defaultValue = "7") int days) {
        return dashboardService.getActivity(days);
    }
}