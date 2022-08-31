package fr.sparkit.accounting.services;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.FileDto;
import fr.sparkit.accounting.entities.DocumentAccountAttachement;

@Service
public interface IStorageService {
    void store(List<FileDto> filesDto);

    Resource loadFile(String filename);

    boolean deleteAttachementFiles(List<Long> filesIds);

    void init();

    List<DocumentAccountAttachement> getDocumentAccountAttachements(Long id);
}
