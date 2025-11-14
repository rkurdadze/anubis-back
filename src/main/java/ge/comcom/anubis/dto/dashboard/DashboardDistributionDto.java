package ge.comcom.anubis.dto.dashboard;

import lombok.Data;

@Data
public class DashboardDistributionDto {
    private Long typeId;
    private String typeName;
    private Long count;
    private Double percentage;
}