package nextstep.subway.path.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.springframework.stereotype.Service;

import nextstep.subway.line.application.LineService;
import nextstep.subway.line.domain.Section;
import nextstep.subway.path.domain.PathFinder;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.path.dto.PathStationResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;

@Service
public class PathService {

    private final LineService lineService;
    private final StationService stationService;

    public PathService(LineService lineService, StationService stationService) {
        this.lineService = lineService;
        this.stationService = stationService;
    }

    public PathResponse getShortestPaths(Long source, Long target) {
        Station sourceStation = stationService.findStationById(source);
        Station targetStation = stationService.findStationById(target);
        Set<Section> allSection = lineService.findAllSection();
        PathFinder pathFinder = new PathFinder(allSection);
        GraphPath<Station, Section> path = pathFinder.getShortestPaths(sourceStation, targetStation);
        return convertPathResponse(path.getVertexList(), path.getWeight());
    }

    private PathResponse convertPathResponse(List<Station> stations, double weight) {
        return new PathResponse(convertPathStationResponses(stations), (int)weight);
    }

    private List<PathStationResponse> convertPathStationResponses(List<Station> stations) {
        return stations.stream()
            .map(station -> PathStationResponse.of(station.getId(), station.getName(), station.getCreatedDate()))
            .collect(Collectors.toList());
    }
}
