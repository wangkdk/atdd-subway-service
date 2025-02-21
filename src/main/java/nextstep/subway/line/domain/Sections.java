package nextstep.subway.line.domain;

import static nextstep.subway.exception.ExceptionMessage.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import nextstep.subway.exception.BadRequestException;
import nextstep.subway.station.domain.Station;

@Embeddable
public class Sections {

    public static final int SECTION_MIN_COUNT = 1;

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public void add(Section section) {
        sections.add(section);
    }

    public List<Section> getSections() {
        return sections;
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }
        List<Station> stations = new ArrayList<>();
        Section section = extractFirstSection();
        stations.add(section.getUpStation());
        Set<Station> upStations = extractUpStations();
        while (!isLastStation(section.getDownStation(), upStations)) {
            Section extractSection = extractSectionByContainsUpStation(section.getDownStation());
            stations.add(extractSection.getUpStation());
            section = extractSection;
        }
        stations.add(section.getDownStation());
        return stations;
    }

    public void addSection(Section section) {
        if (sections.isEmpty()) {
            sections.add(section);
            return;
        }
        validateAddSection(section);
        if (!addStationOfBetween(section)) {
            sections.add(section);
        }
    }

    public void removeSection(Station station) {
        validateRemoveSection(station);
        Optional<Section> sectionOfUpStation = findSectionOfEqualUpStation(station);
        Optional<Section> sectionOfDownStation = findSectionOfEqualDownStation(station);
        if (sectionOfUpStation.isPresent() && sectionOfDownStation.isPresent()) {
            addConnectSection(sectionOfUpStation.get(), sectionOfDownStation.get());
        }
        sectionOfUpStation.ifPresent(it -> sections.remove(it));
        sectionOfDownStation.ifPresent(it -> sections.remove(it));
    }

    private boolean addStationOfBetween(Section section) {
        Optional<Section> sectionOfEqualUpStation = findSectionOfEqualUpStation(section.getUpStation());
        if (sectionOfEqualUpStation.isPresent()) {
            addUpStationOfBetween(section, sectionOfEqualUpStation.get());
            return true;
        }
        Optional<Section> sectionOfEqualDownStation = findSectionOfEqualDownStation(section.getDownStation());
        if (sectionOfEqualDownStation.isPresent()) {
            addDownStationOfBetween(section, sectionOfEqualDownStation.get());
            return true;
        }
        return false;
    }

    private boolean isLastStation(Station station, Set<Station> upStations) {
        return !upStations.contains(station);
    }

    private Section extractSectionByContainsUpStation(Station station) {
        return sections.stream()
            .filter(section -> section.getUpStation().equals(station))
            .findFirst()
            .orElseThrow(() -> new BadRequestException(NON_EXIST_STATION_TO_SECTION));
    }

    private Section extractFirstSection() {
        Set<Station> downStations = extractDownStations();
        return sections.stream()
            .filter(section -> !downStations.contains(section.getUpStation()))
            .findFirst()
            .orElseThrow(() -> new BadRequestException(NON_EXIST_STATION_TO_SECTION));
    }

    private Set<Station> extractAllStations() {
        Set<Station> allStations = extractUpStations();
        allStations.addAll(extractDownStations());
        return allStations;
    }

    private Set<Station> extractUpStations() {
        return sections.stream()
            .map(Section::getUpStation)
            .collect(Collectors.toSet());
    }

    private Set<Station> extractDownStations() {
        return sections.stream()
            .map(Section::getDownStation)
            .collect(Collectors.toSet());
    }

    private void addUpStationOfBetween(Section section, Section findSection) {
        findSection.updateUpStation(section.getDownStation(), section.getDistance());
        sections.add(section);
    }

    private void addDownStationOfBetween(Section section, Section findSection) {
        findSection.updateDownStation(section.getUpStation(), section.getDistance());
        sections.add(section);
    }

    private void validateAddSection(Section section) {
        Set<Station> allStations = extractAllStations();
        validateDuplicate(section, allStations);
        validateNonExist(section, allStations);
    }

    private void validateDuplicate(Section section, Set<Station> allStations) {
        if (allStations.contains(section.getUpStation())
            && allStations.contains(section.getDownStation())) {
            throw new BadRequestException(ALREADY_ADD_SECTION);
        }
    }

    private void validateNonExist(Section section, Set<Station> allStations) {
        if (!allStations.contains(section.getUpStation())
            && !allStations.contains(section.getDownStation())) {
            throw new BadRequestException(NOT_POSSIBLE_ADD_SECTION);
        }
    }

    private void validateRemoveSection(Station station) {
        validateRemoveSectionSize();
        validateExistStation(station);
    }

    private void validateRemoveSectionSize() {
        if (sections.size() <= SECTION_MIN_COUNT) {
            throw new BadRequestException(NOT_REMOVE_SECTION_MIN_SIZE);
        }
    }

    private void validateExistStation(Station station) {
        if (!extractAllStations().contains(station)) {
            throw new BadRequestException(NON_EXIST_STATION_TO_SECTION);
        }
    }

    private void addConnectSection(Section sectionOfUpStation, Section sectionOfDownStation) {
        Station newUpStation = sectionOfDownStation.getUpStation();
        Station newDownStation = sectionOfUpStation.getDownStation();
        int newDistance = sectionOfUpStation.getDistance() + sectionOfDownStation.getDistance();
        sections.add(new Section(sectionOfUpStation.getLine(), newUpStation, newDownStation, newDistance));
    }

    private Optional<Section> findSectionOfEqualUpStation(Station station) {
        return sections.stream()
            .filter(it -> it.getUpStation().equals(station))
            .findFirst();
    }

    private Optional<Section> findSectionOfEqualDownStation(Station station) {
        return sections.stream()
            .filter(it -> it.getDownStation().equals(station))
            .findFirst();
    }
}
