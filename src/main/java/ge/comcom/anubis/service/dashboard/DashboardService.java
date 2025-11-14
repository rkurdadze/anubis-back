package ge.comcom.anubis.service.dashboard;

import ge.comcom.anubis.dto.dashboard.*;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ObjectRepository objectRepository;
    private final ObjectTypeRepository typeRepository;
    private final ObjectVersionRepository objectVersionRepository;

    public List<DashboardDistributionDto> getDistribution() {
        List<Object[]> raw = objectRepository.countByType();

        long total = raw.stream()
                .mapToLong(r -> (Long) r[1])
                .sum();

        Map<Long, String> typeNames = typeRepository.findAllNames();

        List<DashboardDistributionDto> list = new ArrayList<>();

        for (Object[] row : raw) {
            Long typeId = (Long) row[0];
            Long count = (Long) row[1];

            DashboardDistributionDto dto = new DashboardDistributionDto();
            dto.setTypeId(typeId);
            dto.setTypeName(typeNames.getOrDefault(typeId, "Тип #" + typeId));
            dto.setCount(count);
            dto.setPercentage(total == 0 ? 0.0 : (count * 100.0 / total));

            list.add(dto);
        }

        return list;
    }

    public DashboardActivityDto getActivity(int days) {
        LocalDate today = LocalDate.now();

        List<DashboardActivityDayDto> result = new ArrayList<>();

        long max = 0;
        long total = 0;

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            long count = objectVersionRepository.countCreatedObjectsByDay(date);

            if (count > max) max = count;
            total += count;

            String label = date.getDayOfMonth() + " " +
                    date.getMonth().getDisplayName(TextStyle.SHORT, new Locale("ru"));

            result.add(new DashboardActivityDayDto(label, count));
        }

        DashboardActivityDto dto = new DashboardActivityDto();
        dto.setDays(result);
        dto.setTotal(total);
        dto.setMax(max);

        return dto;
    }
}