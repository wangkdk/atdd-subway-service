package nextstep.subway.path.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nextstep.subway.line.application.LineService;
import nextstep.subway.line.domain.Section;
import nextstep.subway.member.domain.MemberAgeType;
import nextstep.subway.path.domain.PathFinder;
import nextstep.subway.path.domain.SubwayFare;
import nextstep.subway.path.dto.PathFinderResponse;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.path.dto.PathStationResponse;
import nextstep.subway.path.dto.SubwayFareRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;

@Service
public class PathService {

    private final LineService lineService;
    private final StationService stationService;
    private final PathFinder pathFinder;

    public PathService(LineService lineService, StationService stationService, PathFinder pathFinder) {
        this.lineService = lineService;
        this.stationService = stationService;
        this.pathFinder = pathFinder;
    }

    public PathResponse getShortestPaths(Long source, Long target, int age) {
        Station sourceStation = stationService.findStationById(source);
        Station targetStation = stationService.findStationById(target);
        Set<Section> allSection = lineService.findAllSection();
        PathFinderResponse pathFinderResponse = pathFinder.getShortestPaths(allSection, sourceStation, targetStation);
        int subwayUsageFare = SubwayFare.getSubwayUsageFare(new SubwayFareRequest(
            pathFinderResponse.getDistance(), pathFinderResponse.getLineSurcharge(),
            MemberAgeType.getMemberAgeType(age)));

        return convertPathResponse(pathFinderResponse.getStations(), pathFinderResponse.getDistance(),
            subwayUsageFare);
    }

    private PathResponse convertPathResponse(List<Station> stations, double weight, int subwayFare) {
        return new PathResponse(convertPathStationResponses(stations), (int)weight, subwayFare);
    }

    private List<PathStationResponse> convertPathStationResponses(List<Station> stations) {
        return stations.stream()
            .map(station -> PathStationResponse.of(station.getId(), station.getName(), station.getCreatedDate()))
            .collect(Collectors.toList());
    }
}
