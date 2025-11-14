package ge.comcom.anubis.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardActivityDayDto {
    private String label;
    private Long count;
}