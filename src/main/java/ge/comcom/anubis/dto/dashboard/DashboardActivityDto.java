package ge.comcom.anubis.dto.dashboard;

import lombok.Data;

import java.util.List;

@Data
public class DashboardActivityDto {
    private List<DashboardActivityDayDto> days;
    private Long total;
    private Long max;
}