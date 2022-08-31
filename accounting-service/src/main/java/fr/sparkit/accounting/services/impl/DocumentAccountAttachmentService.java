package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dao.DocumentAccountAttachementDao;
import fr.sparkit.accounting.dao.DocumentAccountDao;
import fr.sparkit.accounting.dto.DocumentAccountAttachementDto;
import fr.sparkit.accounting.dto.FileDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountAttachement;
import fr.sparkit.accounting.services.IStorageService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service("DocumentAccountAttachmentService")
@Slf4j
public class DocumentAccountAttachmentService extends GenericService<DocumentAccountAttachement, Long>
        implements IStorageService {

    @Value("${document.account-attachement.storage-directory}")
    private Path rootLocation;
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;

    private static final String SEPARATOR = "_";
    private final DocumentAccountDao documentAccountDao;
    private final DocumentAccountAttachementDao documentAccountAttachementDao;

    @Autowired
    public DocumentAccountAttachmentService(DocumentAccountDao documentAccountDao,
            DocumentAccountAttachementDao documentAccountAttachementDao) {
        this.documentAccountDao = documentAccountDao;
        this.documentAccountAttachementDao = documentAccountAttachementDao;
    }

    @Override
    public void store(List<FileDto> filesDto) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.DATE_FORMAT_YY_MM_DD_HH_MM_SS);
        String formatDateTime = now.format(formatter);

        init();
        for (FileDto fileDto : filesDto) {
            Optional<DocumentAccount> documentAccount = documentAccountDao.findById(fileDto.getDocumentAccountId());

            if (!documentAccount.isPresent()) {
                log.error(DOCUMENT_ACCOUNT_NON_EXISTENT, fileDto.getDocumentAccountId());
                throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_NON_EXISTENT);
            }

            String newFileName = String.valueOf(fileDto.getDocumentAccountId()).concat(SEPARATOR).concat(formatDateTime)
                    .concat(SEPARATOR).concat(fileDto.getName());

            try (FileOutputStream stream = new FileOutputStream(
                    rootLocation.toString().concat(File.separator).concat(newFileName))) {

                stream.write(Base64.getDecoder().decode(fileDto.getBase64File()));

                DocumentAccountAttachementDto documentAccountAttachementDto = new DocumentAccountAttachementDto(
                        fileDto.getId(), newFileName, documentAccount.get());

                saveDocumentAccountAttachement(documentAccountAttachementDto);

                log.info(SUCCESS_FILE_UPLOADED);
                log.info(ORIGINAL_FILE_NEW_FILE, fileDto.getName(), newFileName);

            } catch (IOException e) {
                log.error(UNEXPECTED_ERROR_UPLOAD_FILE, e);
                throw new HttpCustomException(
                        ApiErrors.Accounting.DOCUMENT_ACCOUNT_UPLOAD_DOCUMENT_ACCOUNT_ATTACHEMENT_FAIL);
            }
        }

    }

    @Override
    public Resource loadFile(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                log.error(CANNOT_FIND_FILE);
                throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_ATTACHEMENT_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            log.error(UNEXPECTED_ERROR_LOAD_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_ATTACHEMENT_ERROR_LOAD);
        }
    }

    @Override
    public boolean deleteAttachementFiles(List<Long> filesIds) {
        List<DocumentAccountAttachement> documentAccountAttachements = documentAccountAttachementDao
                .findByIdIn(filesIds);
        documentAccountAttachements.forEach((DocumentAccountAttachement attachement) -> {
            DocumentAccountAttachement documentAccountAttachement = documentAccountAttachementDao
                    .findByDocumentAccountIdAndAndFileNameAndIsDeletedFalse(attachement.getDocumentAccount().getId(),
                            attachement.getFileName());
            if (documentAccountAttachement != null) {
                delete(documentAccountAttachement.getId());
                File fileToDelete = new File(
                        rootLocation.toString().concat(File.separator).concat(attachement.getFileName()));
                try {
                    Files.deleteIfExists(fileToDelete.toPath());
                } catch (IOException e) {
                    log.error(UNEXPECTED_ERROR_DELETE_FILE, e);
                    throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_ATTACHEMENT_ERROR_DELETING);
                }
            } else {
                log.error(DOCUMENT_ACCOUNT_ATT_NOT_FOUND);
                throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_ATTACHEMENT_NOT_FOUND);
            }
        });
        return true;
    }

    @Override
    public void init() {
        try {
            if (!rootLocation.toFile().exists()) {
                Files.createDirectories(rootLocation);
            }
            if (!excelStoragePath.toFile().exists()) {
                Files.createDirectories(excelStoragePath);
            }
        } catch (IOException e) {
            log.error(LOG_INITIALIZE_STORAGE_EXCEPTION_MSG);
            log.error(AccountingConstants.LOG_INITIALIZE_STORAGE_EXCEPTION, e);
        }
    }

    public void saveDocumentAccountAttachement(DocumentAccountAttachementDto documentAccountAttachementDto) {
        DocumentAccountAttachement documentAccountAttachementCreated = new DocumentAccountAttachement();
        documentAccountAttachementCreated.setDocumentAccount(documentAccountAttachementDto.getDocumentAccount());
        documentAccountAttachementCreated.setFileName(documentAccountAttachementDto.getFileName());
        documentAccountAttachementCreated.setDeleted(false);
        documentAccountAttachementCreated.setDeletedToken(null);

        documentAccountAttachementCreated = saveAndFlush(documentAccountAttachementCreated);

        log.info(LOG_ENTITY_CREATED, documentAccountAttachementCreated);

    }

    public List<DocumentAccountAttachement> getDocumentAccountAttachements(Long documentAccountId) {
        return documentAccountAttachementDao.findByDocumentAccountIdAndIsDeletedFalse(documentAccountId);
    }
}
