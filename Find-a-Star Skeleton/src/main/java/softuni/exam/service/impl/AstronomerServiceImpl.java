package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.xmls.AstronomerRootDto;
import softuni.exam.models.dto.xmls.AstronomerSeedDto;
import softuni.exam.models.entity.Astronomer;
import softuni.exam.models.entity.Star;
import softuni.exam.repository.AstronomerRepository;
import softuni.exam.repository.StarRepository;
import softuni.exam.service.AstronomerService;
import softuni.exam.util.ValidationUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class AstronomerServiceImpl implements AstronomerService {
    private static final String FILE_PATH = "src/main/resources/files/xml/astronomers.xml";
    private final AstronomerRepository astronomerRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final StarRepository starRepository;

    public AstronomerServiceImpl(AstronomerRepository astronomerRepository, ModelMapper modelMapper, ValidationUtil validationUtil, StarRepository starRepository) {
        this.astronomerRepository = astronomerRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.starRepository = starRepository;
    }

    @Override
    public boolean areImported() {
        return this.astronomerRepository.count() > 0;
    }

    @Override
    public String readAstronomersFromFile() throws IOException {
        return new String(Files.readAllBytes(Path.of(FILE_PATH)));
    }

    @Override
    public String importAstronomers() throws IOException, JAXBException {
        StringBuilder sb = new StringBuilder();

        JAXBContext context = JAXBContext.newInstance(AstronomerRootDto.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        AstronomerRootDto astronomerRootDto = (AstronomerRootDto) unmarshaller.unmarshal(new File(FILE_PATH));

        for (AstronomerSeedDto astronomerSeedDto : astronomerRootDto.getAstronomerSeedDtoList()) {

            Optional<Astronomer> optionalAstronomer =  this.astronomerRepository.findByFirstNameAndLastName(astronomerSeedDto.getFirstName(), astronomerSeedDto.getLastName());
            Optional<Star> optionalStar = this.starRepository.findById(astronomerSeedDto.getStar());

            if(!this.validationUtil.isValid(astronomerSeedDto) || optionalAstronomer.isPresent() || optionalStar.isEmpty()){
                sb.append("Invalid astronomer\n");
                continue;
            }
            Astronomer astronomer = this.modelMapper.map(astronomerSeedDto, Astronomer.class);
            astronomer.setObservingStar(optionalStar.get());

            sb.append(String.format("Successfully imported astronomer %s %s - %.2f\n",
                    astronomer.getFirstName(), astronomer.getLastName(), astronomer.getAverageObservationHours()));

            this.astronomerRepository.saveAndFlush(astronomer);
        }

        return sb.toString();
    }
}
