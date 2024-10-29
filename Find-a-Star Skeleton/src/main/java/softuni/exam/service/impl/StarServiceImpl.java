package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.jsons.StarSeedDto;
import softuni.exam.models.entity.Star;
import softuni.exam.models.entity.StarType;
import softuni.exam.repository.ConstellationRepository;
import softuni.exam.repository.StarRepository;
import softuni.exam.service.StarService;
import softuni.exam.util.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StarServiceImpl implements StarService {
    private static final String FILE_PATH = "src/main/resources/files/json/stars.json";
    private final StarRepository starRepository;
    private final Gson gson;
    private final ModelMapper modelMapper;
    private final ConstellationRepository constellationRepository;
    private final ValidationUtil validationUtil;

    public StarServiceImpl(StarRepository starRepository, Gson gson, ModelMapper modelMapper, ConstellationRepository constellationRepository, ValidationUtil validationUtil) {
        this.starRepository = starRepository;
        this.gson = gson;
        this.modelMapper = modelMapper;
        this.constellationRepository = constellationRepository;
        this.validationUtil = validationUtil;
    }

    @Override
    public boolean areImported() {
        return this.starRepository.count() > 0;
    }

    @Override
    public String readStarsFileContent() throws IOException {
        return new String(Files.readAllBytes(Path.of(FILE_PATH)));
    }

    @Override
    public String importStars() throws IOException {
        StringBuilder sb = new StringBuilder();
        StarSeedDto[] starSeedDtos = this.gson.fromJson(readStarsFileContent(), StarSeedDto[].class);

        for (StarSeedDto starSeedDto : starSeedDtos) {
            Optional<Star> optional = starRepository.findByName(starSeedDto.getName());

            if (!this.validationUtil.isValid(starSeedDto) || optional.isPresent()){
                sb.append(String.format("Invalid star\n"));
                continue;
            }
            Star star = this.modelMapper.map(starSeedDto, Star.class);
            star.setStarType(StarType.valueOf(starSeedDto.getStarType()));
            star.setConstellation(this.constellationRepository.findById(starSeedDto.getConstellation()).get());

            this.starRepository.saveAndFlush(star);
            sb.append(String.format("Successfully imported star %s - %.2f light years\n", star.getName(), star.getLightYears()));
        }

        return sb.toString().trim();
    }

    @Override
    public String exportStars() {
        return this.starRepository
                .findAllByStarTypeOrderByLightYears()
                .stream()
                .map(star -> String.format("Star: %s\n" +
                        "   *Distance: %.2f light years\n" +
                        "   **Description: %s\n" +
                        "   ***Constellation: %s\n",
                        star.getName(), star.getLightYears(), star.getDescription(), star.getConstellation().getName()))
                .collect(Collectors.joining());
    }
}
